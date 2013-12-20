(ns channels.server.api.resources.a-discussion
  (:require [channels.server.api.shared :refer [acceptable-types]]
            [channels.server.api.uri :as uri]
            [channels.server.api.util :refer [maps-for-html error-response pretty-json select-accept-type]]
            [compojure.core :refer [GET]]
            [net.cgrand.enlive-html :as h]
            [resourceful :refer [resource]]
            [channels.server.api.db :as db]))

(h/deftemplate html-template "templates/a_discussion.html"
  [context channel discussion]
  [:html h/text-node] (h/replace-vars (assoc (maps-for-html context channel discussion)
                                             ;; TODO: TEMP HARD-CODED VALUE
                                             :message-count "2"))
  [:a#channel] (h/set-attr :href (uri/a-channel (:id channel)))
  [:a#discussions] (h/set-attr :href (uri/discussions (:id channel)))
  [:a#messages] (h/set-attr :href (uri/messages (:id channel) (:id discussion))))

(defn represent [accept-header channel-id discussion-id context]
  (case (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"}
           :body (let [[channel discussion] (db/get-multi [channel-id discussion-id])]
                   (html-template context channel discussion))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (pretty-json (db/get-doc discussion-id))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (resource "a discussion" "/channels/:channel-id/discussions/:discussion-id"
    (GET
      {{:keys [channel-id discussion-id]} :params
       {accept-header "accept"} :headers}
      ;; TODO: some 404 checks!
      (represent accept-header channel-id discussion-id context))))
