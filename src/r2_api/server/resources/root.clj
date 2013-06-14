(ns r2-api.server.resources.root
  (:require [r2-api.server.util :refer [error-response pretty-json select-accept-type]]
            [compojure.core :refer [GET routes]]
            [r2-api.server.templates :as t]))

(def acceptable-types #{"application/json" "text/html"})

(def links [{:href "groups", :text "Groups"}
            {:href "", :text "People (coming soon)"}
            {:href "", :text "Webhooks (coming soon)"}])

(defn to-json [context links]
  (pretty-json {:server {:name (:server-name context)}
                :links links}))

(defn represent [accept-header context links]
  (condp = (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (t/root context links)}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (to-json context links)}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/" {{accept-header "accept"} :headers}
      (represent accept-header context links))))
