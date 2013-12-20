(ns channels.server.api.resources.a-user
  (:require [channels.server.api.shared :refer [acceptable-types]]
            [channels.server.api.util :refer [maps-for-html error-response pretty-json select-accept-type]]
            [compojure.core :refer [GET]]
            [net.cgrand.enlive-html :as h]
            [resourceful :refer [resource]]
            [channels.server.api.db :as db]))

(h/deftemplate html-template "templates/a_user.html"
  [context user]
  [:html h/text-node] (h/replace-vars (assoc context :user-name (:name user))))

(defn represent [accept-header context user]
  (case (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (html-template context user)}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (pretty-json user)}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (resource "a user" "/users/:user-id"
    (GET
      {{user-id :user-id} :params
       {accept-header "accept"} :headers}
      (if-let [user (db/get-doc user-id)]
              (represent accept-header context user)
              (error-response 404 "Not found.")))))
