(ns r2-api.server.resources.a-group
  (:require [r2-api.server.util :refer [acceptable? error-response pretty-json select-accept-type type-supported?]]
            [compojure.core :refer [GET routes]]
            [r2-api.server.templates :as t]
            [r2-api.server.db :as db]))

(def acceptable-types #{"application/json" "text/html"})

(defn group-to-json [group]
  (-> group
      (assoc ,,, :id (:_id group))
      (dissoc ,,, :_id :_rev)
      pretty-json))

(defn represent-group [accept-header group-id context]
  (condp = (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (t/a-group context (db/get-doc group-id))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (group-to-json (db/get-doc group-id))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-a-group-resource-handler [context]
  (routes
    (GET "/groups/:group-id"
      {{group-id :group-id} :params
       {accept-header "accept"} :headers}
      (represent-group accept-header group-id context))))
