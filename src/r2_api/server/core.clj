(ns r2-api.server.core
    (:require [r2-api.server.resources.root :as root]
              [r2-api.server.resources.groups :as groups]
              [r2-api.server.resources.a-group :as a-group]
              [r2-api.server.resources.discussions :as discussions]
              [r2-api.server.resources.a-discussion :as a-discussion]
              [r2-api.server.resources.messages :as messages]
              [r2-api.server.resources.a-message :as a-message]
              [r2-api.server.templates :as t]
              [r2-api.server.db :as db]
              [compojure.core :refer [defroutes]]
              [compojure.handler :as ch]
              [ring.adapter.jetty :as rj]
              [clj-time.core :refer [now]]
              [slugger.core :refer [->slug]]
              [clojure.string :refer [blank?]]
              [ring.middleware.json :refer [wrap-json-params]]
              [ring.middleware.head :refer [wrap-head]]
              [cheshire.core :as json]
              [clj-time.format :refer [formatters unparse]]))

(def ^:private context {:server-name "Avi’s R2"})

(defroutes routes
  (root/create-handler context)
  (groups/create-handler context)
  (a-group/create-handler context)
  (discussions/create-handler context)
  (a-discussion/create-handler context)
  (messages/create-handler context)
  (a-message/create-handler context))

(def ring-handler
  "this is a var so it can be used by lein-ring"
  (-> (ch/api routes)
      wrap-json-params
      ;; TODO: there appears to be a bug in wrap-head such that Content-Length gets set to 0
      wrap-head))

(defn -main [& args]
  (println "starting Web server")
  (rj/run-jetty ring-handler {:port 3000 :join? false}))
