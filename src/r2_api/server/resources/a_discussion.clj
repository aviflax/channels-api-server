(ns r2-api.server.resources.a-discussion
  (:require [r2-api.server.util :refer [doc-to-json error-response pretty-json select-accept-type]]
            [compojure.core :refer [GET routes]]
            [r2-api.server.templates :as t]
            [r2-api.server.db :as db]))

(def acceptable-types #{"application/json" "text/html"})

(defn represent [accept-header group-id discussion-id context]
  (condp = (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"}
           :body (let [[group discussion] (db/get-multi [group-id discussion-id])]
                   (t/a-discussion context group discussion))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (doc-to-json (db/get-doc discussion-id))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/groups/:group-id/discussions/:discussion-id"
      {{:keys [group-id discussion-id]} :params
       {accept-header "accept"} :headers}
      (represent accept-header group-id discussion-id context))))
