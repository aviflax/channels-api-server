(ns channels.server.api.resources.a-message
  (:require [channels.server.api.shared :refer [acceptable-types]]
            [channels.server.api.util :refer [attr-append maps-for-html doc-to-json error-response select-accept-type]]
            [compojure.core :refer [GET routes]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]))

(defn uri [channel-id discussion-id message-id] (str "/channels/" channel-id "/discussions/" discussion-id "/messages/" message-id))

(h/deftemplate html-template "templates/a_message.html"
  [context channel discussion message]
  [:html h/text-node] (h/replace-vars (maps-for-html context channel discussion))
  [:span.message-id] (h/content (:message-id context))
  [:a#channel] (attr-append :href str (:_id channel))
  [:a#discussions] (h/set-attr :href (str "/channels/" (:_id channel) "/discussions"))
  [:a#discussion] (h/set-attr :href (str "/channels/" (:_id channel) "/discussions/" (:_id discussion)))
  [:a#messages] (h/set-attr :href (str "/channels/" (:_id channel) "/discussions/" (:_id discussion) "/messages"))
  [:article :pre] (h/content (:body message))
  [:#username] (h/content (get-in message [:user :name]))
  [:a#user] (h/do->
              (h/set-attr :href (str "/people/" (get-in message [:user :id])))
              (h/content (get-in message [:user :name])))
  [:.message-date] (h/content (:created message)))

(defn represent [accept-header channel-id discussion-id message-id context]
  (case (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"}
           :body (apply html-template (concat [context] (db/get-multi [channel-id discussion-id message-id])))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (doc-to-json (db/get-doc message-id))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/channels/:channel-id/discussions/:discussion-id/messages/:message-id"
      {{:keys [channel-id discussion-id message-id]} :params
       {accept-header "accept"} :headers}
      (represent accept-header channel-id discussion-id message-id context))))
