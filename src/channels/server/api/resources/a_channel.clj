(ns channels.server.api.resources.a-channel
  (:require [channels.server.api.shared :refer [acceptable-types]]
            [channels.server.api.util :refer [maps-for-html error-response pretty-json resource select-accept-type]]
            [channels.server.api.uri :as uri]
            [compojure.core :refer [GET]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]))

(h/deftemplate html-template "templates/a_channel.html"
  [context channel]
  [:html h/text-node] (h/replace-vars (maps-for-html context channel))
  [:a#discussions] (h/set-attr :href (uri/discussions (:id channel))))

(defn represent [accept-header channel context]
  (case (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (html-template context channel)}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (pretty-json channel)}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (resource "a channel" "/channels/:channel-id"
    (GET
      {{channel-id :channel-id} :params
       {accept-header "accept"} :headers}
      (if-let [channel (db/get-doc channel-id)]
        (represent accept-header channel context)
        (error-response 404 "Not found.")))))
