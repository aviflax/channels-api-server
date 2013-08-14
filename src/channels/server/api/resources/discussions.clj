(ns channels.server.api.resources.discussions
  (:require [channels.server.api.shared :refer [acceptable-types]]
            [channels.server.api.uri :as uri]
            [channels.server.api.util :refer [acceptable? contains-non-blank-string error-response maps-for-html non-blank-string pretty-json resource select-accept-type type-supported?]]
            [compojure.core :refer [GET POST]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]))

(defn to-json
  ([context channel discussions] (to-json context channel discussions nil))
  ([context channel discussions created]
  (let [massage-discussion #(assoc % :href (uri/a-discussion (get-in % [:channel :id]) (:id %))
                                     :id (:id %)
                                     ; TODO: hard-coded
                                     :key-participants [{:name "Avi Flax" :href "/people/avi-flax"}]
                                     ; TODO: hard-coded
                                     :new-messages 23)
        m {:discussions (map massage-discussion discussions)
           :server {:name (:server-name context)}
           :channel channel}
        ; TODO this seems awkward/iffy. Is there a better way to express this?
        m (if created
              (assoc m :created (massage-discussion created))
              m)]
    (pretty-json m))))


(h/deftemplate html-template "templates/discussions.html"
  [context channel discussions created]
  [:html h/text-node] (h/replace-vars (maps-for-html context channel))
  [:ul#discussions :li] (h/clone-for [discussion discussions]
                     [:a] (h/do->
                            (h/set-attr :href (uri/a-discussion (:id channel) (:id discussion)))
                            (h/content (:subject discussion))))
  [:a#channel] (h/set-attr :href (uri/a-channel (:id channel)))
  [:input#channel-id] (h/set-attr :value (:id channel)))


(defn represent
  ([accept-header channel-id context] (represent accept-header channel-id context nil))
  ([accept-header channel-id context created]
    (let [data [context (db/get-doc channel-id) (db/get-discussions channel-id) created]]
      (case (select-accept-type acceptable-types accept-header)
        :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (apply html-template data)}
        :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (apply to-json data)}
        (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))))


(defn handle-post [context headers {:keys [channel-id subject body] :as params} user-id]
  (let [user (when (contains-non-blank-string params :user_id)
                         (db/get-doc user-id))]
    (cond
      (not (contains? headers "content-type"))
      (error-response 400 "The request must include the header Content-Type.")

      (not (type-supported? ["application/json" "application/x-www-form-urlencoded"] (get headers "content-type")))
      (error-response 415 "The request representation must be of the type application/json or application/x-www-form-urlencoded.")

      (not (non-blank-string subject))
      (error-response 400 "The request must include the string parameter or property 'subject', and it may not be null or blank.")

      (not (acceptable? acceptable-types (get headers "accept")))
      (error-response 406 "Not Acceptable; available content types are text/html and application/json.")

      (and (contains-non-blank-string params :user_id)
           (not user))
      (error-response 400 "The user specified in 'user_id' does not exist.")

      (and (contains-non-blank-string params :body)
           (not (contains-non-blank-string params :user_id)))
      (error-response 400 "If the parameter 'body' is specified then 'user_id' is required as well.")

      (and (contains-non-blank-string params :user_id)
           (not (contains-non-blank-string params :body)))
      (error-response 400 "If the parameter 'user_id' is specified then 'body' is required as well.")

      :default
      (let [discussion (db/create-discussion! subject channel-id)]
        (when (every? (partial contains-non-blank-string params) [:body :user_id])
          ;; request contains data for initial message, so create that right now
          (db/create-message! channel-id (:id discussion) user body))
        (-> (represent (get headers "accept") channel-id context discussion)
            (assoc ,,, :status 201)
            (assoc-in ,,, [:headers "Location"] (uri/a-discussion channel-id (:id discussion))))))))


(defn create-handler [context]
  (resource "/channels/:channel-id/discussions"
    (GET
      {{channel-id :channel-id} :params
       {accept-header "accept"} :headers}
      (represent accept-header channel-id context))

    (POST
      {headers :headers
       params :params
       {user-id :user_id} :params}
      (handle-post context headers params user-id))))
