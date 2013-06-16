(ns r2-api.server.resources.a-message
  (:require [r2-api.server.util :refer [attr-append combine doc-to-json error-response select-accept-type]]
            [compojure.core :refer [GET routes]]
            [net.cgrand.enlive-html :as h]
            [r2-api.server.db :as db]))

(def acceptable-types #{"application/json" "text/html"})

(defn uri [group-id discussion-id message-id] (str "/groups/" group-id "/discussions/" discussion-id "/messages/" message-id))

(h/deftemplate html-template "templates/a_message.html"
  [context group discussion message]
  [:html h/text-node] (h/replace-vars (combine context group discussion))
  [:span.message-id] (h/content (:message-id context))
  [:a#group] (attr-append :href str (:_id group))
  [:a#discussions] (h/set-attr :href (str "/groups/" (:_id group) "/discussions"))
  [:a#discussion] (h/set-attr :href (str "/groups/" (:_id group) "/discussions/" (:_id discussion)))
  [:a#messages] (h/set-attr :href (str "/groups/" (:_id group) "/discussions/" (:_id discussion) "/messages"))
  [:article :pre] (h/content (:body message))
  [:#username] (h/content (get-in message [:user :name]))
  [:a#user] (h/do->
              (h/set-attr :href (str "/people/" (get-in message [:user :id])))
              (h/content (get-in message [:user :name])))
  [:.message-date] (h/content (:created message)))

(defn represent [accept-header group-id discussion-id message-id context]
  (condp = (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"}
           :body (apply html-template (concat [context] (db/get-multi [group-id discussion-id message-id])))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (doc-to-json (db/get-doc message-id))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/groups/:group-id/discussions/:discussion-id/messages/:message-id"
      {{:keys [group-id discussion-id message-id]} :params
       {accept-header "accept"} :headers}
      (represent accept-header group-id discussion-id message-id context))))
