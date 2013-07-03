(ns channels.server.api.resources.a-channel
  (:require [channels.server.api.util :refer [maps-for-html doc-to-json error-response select-accept-type]]
            [channels.server.api.resources.discussions :as discussions]
            [compojure.core :refer [GET routes]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]))

(def acceptable-types #{"application/json" "text/html"})

(defn uri [channel-id] (str "/channels/" channel-id))

(h/deftemplate html-template "templates/a_channel.html"
  [context channel]
  [:html h/text-node] (h/replace-vars (maps-for-html context channel))
  [:a#discussions] (h/set-attr :href (discussions/uri (:_id channel))))

(defn represent [accept-header channel context]
  (case (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (html-template context channel)}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (doc-to-json channel)}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/channels/:channel-id"
      {{channel-id :channel-id} :params
       {accept-header "accept"} :headers}
      (let [channel (db/get-doc channel-id)]
        (cond
          (nil? channel)
          (error-response 404 "Not found.")

          :default
          (represent accept-header channel context))))))
