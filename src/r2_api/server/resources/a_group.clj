(ns r2-api.server.resources.a-group
  (:require [r2-api.server.util :refer [combine doc-to-json error-response select-accept-type]]
            [r2-api.server.resources.discussions :as discussions]
            [compojure.core :refer [GET routes]]
            [net.cgrand.enlive-html :as h]
            [r2-api.server.db :as db]))

(def acceptable-types #{"application/json" "text/html"})

(defn uri [group-id] (str "/groups/" group-id))

(h/deftemplate html-template "templates/a_group.html"
  [context group]
  [:html h/text-node] (h/replace-vars (combine context group))
  [:a#discussions] (h/set-attr :href (discussions/uri (:_id group))))

(defn represent [accept-header group context]
  (case (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (html-template context group)}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (doc-to-json group)}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/groups/:group-id"
      {{group-id :group-id} :params
       {accept-header "accept"} :headers}
      (let [group (db/get-doc group-id)]
        (cond
          (nil? group)
          (error-response 404 "Not found.")

          :default
          (represent accept-header group context))))))
