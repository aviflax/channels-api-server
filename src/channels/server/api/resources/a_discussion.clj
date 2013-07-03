(ns channels.server.api.resources.a-discussion
  (:require [channels.server.api.util :refer [attr-append maps-for-html doc-to-json error-response select-accept-type]]
            [compojure.core :refer [GET routes]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]))

(def acceptable-types #{"application/json" "text/html"})

(defn uri [channel-id discussion-id] (str "/channels/" channel-id "/discussions/" discussion-id))

(h/deftemplate html-template "templates/a_discussion.html"
  [context channel discussion]
  [:html h/text-node] (h/replace-vars (assoc (maps-for-html context channel discussion)
                                             ;; TODO: TEMP HARD-CODED VALUE
                                             :message-count "2"))
  [:a#channel] (attr-append :href str (:_id channel))
  ;; TODO: instead of building the URLs entirely here in the code, it’d be better to have a version of
  ;; replace-vars which can replace vars in an attribute, or to just call functions which build the URLs
  [:a#discussions] (h/set-attr :href (str "/channels/" (:_id channel) "/discussions"))
  [:a#messages] (h/set-attr :href (str "/channels/" (:_id channel) "/discussions/" (:_id discussion) "/messages")))

(defn represent [accept-header channel-id discussion-id context]
  (case (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"}
           :body (let [[channel discussion] (db/get-multi [channel-id discussion-id])]
                   (html-template context channel discussion))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (doc-to-json (db/get-doc discussion-id))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/channels/:channel-id/discussions/:discussion-id"
      {{:keys [channel-id discussion-id]} :params
       {accept-header "accept"} :headers}
      (represent accept-header channel-id discussion-id context))))
