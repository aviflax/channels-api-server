(ns r2-api.server.resources.a-discussion
  (:require [r2-api.server.util :refer [attr-append combine doc-to-json error-response select-accept-type]]
            [compojure.core :refer [GET routes]]
            [net.cgrand.enlive-html :as h]
            [r2-api.server.db :as db]))

(def acceptable-types #{"application/json" "text/html"})

(defn uri [group-id discussion-id] (str "/groups/" group-id "/discussions/" discussion-id))

(h/deftemplate html-template "templates/a_discussion.html"
  [context group discussion]
  [:html h/text-node] (h/replace-vars (assoc (combine context group discussion)
                                             ;; TODO: TEMP HARD-CODED VALUE
                                             :message-count "2"))
  [:a#group] (attr-append :href str (:_id group))
  ;; TODO: instead of building the URLs entirely here in the code, it’d be better to have a version of
  ;; replace-vars which can replace vars in an attribute, or to just call functions which build the URLs
  [:a#discussions] (h/set-attr :href (str "/groups/" (:_id group) "/discussions"))
  [:a#messages] (h/set-attr :href (str "/groups/" (:_id group) "/discussions/" (:_id discussion) "/messages")))

(defn represent [accept-header group-id discussion-id context]
  (case (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"}
           :body (let [[group discussion] (db/get-multi [group-id discussion-id])]
                   (html-template context group discussion))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (doc-to-json (db/get-doc discussion-id))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/groups/:group-id/discussions/:discussion-id"
      {{:keys [group-id discussion-id]} :params
       {accept-header "accept"} :headers}
      (represent accept-header group-id discussion-id context))))
