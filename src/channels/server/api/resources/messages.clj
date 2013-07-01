(ns channels.server.api.resources.messages
  (:require [channels.server.api.resources [a-message :refer [uri]]
                                     [a-group :as a-group]
                                     [a-discussion :as a-discussion]]
            [channels.server.api.util :refer [acceptable? attr-append combine doc-for-json error-response indexed pretty-json select-accept-type type-supported?]]
            [compojure.core :refer [GET POST routes]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]
            [clojure.string :refer [blank?]]
            [clojure.pprint :refer :all]))

(defn to-json
  ([context group discussion messages] (to-json group discussion messages nil))
  ([context group discussion messages created]
    (let [massage-message #(-> (assoc % :href (uri (:_id group) (:_id discussion) (:_id %)))
                               (dissoc ,,, :_id :_rev :type))
          m {:messages (map massage-message messages)
             ; TODO: move the doc prep somewhere more reusable
             :group (assoc (doc-for-json group) :href (a-group/uri (:_id group)))
             :discussion (assoc (doc-for-json discussion) :href (a-discussion/uri (:_id group) (:_id discussion)))
             :server {:name (:server-name context)}}
          ; TODO this seems awkward/iffy. Is there a better way to express this?
          m (if created
                (assoc m :created (massage-message created))
                m)]
      (pretty-json m))))

(def acceptable-types #{"application/json" "text/html"})

(h/deftemplate html-template "templates/messages.html"
  [context group discussion messages created]
  [:html h/text-node] (h/replace-vars (combine context group discussion))
  [:a#group] (attr-append :href str (:_id group))
  [:a#discussions] (h/set-attr :href (str "/groups/" (:_id group) "/discussions"))
  [:a#discussion] (h/set-attr :href (str "/groups/" (:_id group) "/discussions/" (:_id discussion)))
  [:input#group-id] (h/set-attr :value (:_id group))
  [:input#discussion-id] (h/set-attr :value (:_id discussion))
  [:article.message] (h/clone-for [[i message] (indexed messages 1)]
                       [:a#user] (h/do->
                                   (h/set-attr :href (str "/people/" (get-in message [:user :id])))
                                   (h/content (get-in message [:user :name])))
                       [:pre] (h/content (:body message))
                       [:#date] (h/content (:created message))
                       [:#message-number] (h/content (str i))
                       [:a#message] (h/set-attr :href (apply uri (map :_id [group discussion message])))))


(defn represent
  ([accept-header group-id discussion-id context] (represent accept-header group-id discussion-id context nil))
  ([accept-header group-id discussion-id context created]
    (let [data [context (db/get-doc group-id) (db/get-doc discussion-id) (db/get-messages discussion-id) created]]
      (case (select-accept-type acceptable-types accept-header)
        :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (apply html-template data)}
        :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (apply to-json data)}
        (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))))

(defn create-handler [context]
  (let [path "/groups/:group-id/discussions/:discussion-id/messages"]
    (routes
      (GET path
        {{:keys [group-id discussion-id]} :params
         {accept-header "accept"} :headers}
        (represent accept-header group-id discussion-id context))

      (POST path
        {headers :headers
         {:keys [group-id discussion-id body]} :params}
        (cond
          (not (type-supported? ["application/json" "application/x-www-form-urlencoded"] (get headers "content-type")))
          (error-response 415 "The request representation must be of the type application/json or application/x-www-form-urlencoded.")

          (or (nil? body)
              (not (string? body))
              (blank? body))
          (error-response 400 "The request must include the string parameter or property 'body', and it may not be null or blank.")

          (not (acceptable? acceptable-types (get headers "accept")))
          (error-response 406 "Not Acceptable; available content types are text/html and application/json.")

          :default
          (let [created (db/create-message! group-id discussion-id body)]
            (-> (represent (get headers "accept") group-id discussion-id context created)
                (assoc ,,, :status 201)
                (assoc-in ,,, [:headers "Location"] (uri group-id discussion-id (:_id created))))))))))