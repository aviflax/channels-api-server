(ns channels.server.api.resources.messages
  (:require [channels.server.api.shared :refer [acceptable-types]]
            [channels.server.api.uri :as uri]
          [channels.server.api.util :refer [acceptable? maps-for-html error-response indexed pretty-json resource select-accept-type type-supported?]]
            [compojure.core :refer [GET POST]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]
            [clojure.string :refer [blank?]]
            [clojure.pprint :refer :all]))

(defn to-json
  [context channel discussion messages created]
  (let [add-uri #(assoc % :href (uri/a-message (:id channel) (:id discussion) (:id %)))
        m {:messages (map add-uri messages)
           ; TODO: move the doc prep somewhere more reusable
           :channel (assoc channel :href (uri/a-channel (:id channel)))
           :discussion (assoc discussion :href (uri/a-discussion (:id channel) (:id discussion)))
           :server {:name (:server-name context)}}
        ; TODO this seems awkward/iffy. Is there a better way to express this?
        m (if created
            (assoc m :created (add-uri created))
            m)]
    (pretty-json m)))


(h/deftemplate html-template "templates/messages.html"
  [context channel discussion messages created]
  [:html h/text-node] (h/replace-vars (maps-for-html context channel discussion))
  [:a#channel] (h/set-attr :href (uri/a-channel (:id channel)))
  [:a#discussions] (h/set-attr :href (uri/discussions (:id channel)))
  [:a#discussion] (h/set-attr :href (uri/a-discussion (:id channel) (:id discussion)))
  [:article.message] (h/clone-for [[i message] (indexed messages 1)]
                       [:a#user] (h/do->
                                   (h/set-attr :href (uri/a-user (get-in message [:user :id])))
                                   (h/content (get-in message [:user :name])))
                       [:pre] (h/content (:body message))
                       [:#date] (h/content (:created message))
                       [:#message-number] (h/content (str i))
                       [:a#message] (h/set-attr :href (apply uri/a-message (map :id [channel discussion message])))))


(defn represent
  ([context accept-header channel-id discussion-id] (represent context accept-header channel-id discussion-id nil))
  ([context accept-header channel-id discussion-id created]
    (let [data [context (db/get-doc channel-id) (db/get-doc discussion-id) (db/get-messages discussion-id) created]]
      (case (select-accept-type acceptable-types accept-header)
        :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (apply html-template data)}
        :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (apply to-json data)}
        (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))))


(defn create-resource [context accept-header channel-id discussion-id user-id body]
  (if-let [user (db/get-doc user-id)]
    (let [created (db/create-message! channel-id discussion-id user body)]
      (-> (represent context accept-header channel-id discussion-id created)
          (assoc ,,, :status 201)
          (assoc-in ,,, [:headers "Location"] (uri/a-message channel-id discussion-id (:id created)))))
    (error-response 400 "The specified user does not exist or is not valid.")))


(defn create-handler [context]
  (resource "collection of messages" "/channels/:channel-id/discussions/:discussion-id/messages"
    (GET
      {{:keys [channel-id discussion-id]} :params
       {accept-header "accept"} :headers}
      (represent context accept-header channel-id discussion-id))

    (POST
      {headers :headers
       {accept-header "accept"} :headers
       {:keys [channel-id discussion-id body]} :params
       {user-id :user_id} :params}

      (cond
        (not (contains? headers "content-type"))
        (error-response 400 "The request must include the header Content-Type.")

        (not (type-supported? ["application/json" "application/x-www-form-urlencoded"] (get headers "content-type")))
        (error-response 415 "The request representation must be of the type application/json or application/x-www-form-urlencoded.")

        ;; TODO: these next two chunks violate DRY. Refactor.
        (or (nil? user-id)
            (not (string? user-id))
            (blank? user-id))
        (error-response 400 "The request must include the string parameter or property 'user_id', and it may not be null or blank.")

        (or (nil? body)
            (not (string? body))
            (blank? body))
        (error-response 400 "The request must include the string parameter or property 'body', and it may not be null or blank.")

        (not (acceptable? acceptable-types (get headers "accept")))
        (error-response 406 "Not Acceptable; available content types are text/html and application/json.")

        :default
        (create-resource context accept-header channel-id discussion-id user-id body)))))
