(ns channels.server.api.resources.a-message
  (:require [channels.server.api.shared :refer [acceptable-types]]
            [channels.server.api.uri :as uri]
            [channels.server.api.util :refer [maps-for-html error-response pretty-json resource select-accept-type]]
            [compojure.core :refer [GET]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]))

(h/deftemplate html-template "templates/a_message.html"
  [context channel discussion message]
  [:html h/text-node] (h/replace-vars (maps-for-html context channel discussion))
  [:span.message-id] (h/content (:message-id context))
  [:a#channel] (h/set-attr :href (uri/a-channel (:id channel)))
  [:a#discussions] (h/set-attr :href (uri/discussions (:id channel)))
  [:a#discussion] (h/set-attr :href (uri/a-discussion (:id channel) (:id discussion)))
  [:a#messages] (h/set-attr :href (uri/messages (:id channel) (:id discussion)))
  [:article :pre] (h/content (:body message))
  [:#username] (h/content (get-in message [:user :name]))
  [:a#user] (h/do->
              (h/set-attr :href (uri/a-user (get-in message [:user :id])))
              (h/content (get-in message [:user :name])))
  [:.message-date] (h/content (:created message)))

(defn represent [accept-header channel-id discussion-id message-id context]
  (case (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"}
           :body (apply html-template (concat [context] (db/get-multi [channel-id discussion-id message-id])))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (pretty-json (db/get-doc message-id))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (resource "a message" "/channels/:channel-id/discussions/:discussion-id/messages/:message-id"
    (GET
      {{:keys [channel-id discussion-id message-id]} :params
       {accept-header "accept"} :headers}
      ;; TODO: some 404 checks!
      (represent accept-header channel-id discussion-id message-id context))))
