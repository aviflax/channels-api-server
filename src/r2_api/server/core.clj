(ns r2-api.server.core
    (:require [r2-api.server.resources.groups :as groups]
              [r2-api.server.resources.a-group :as a-group]
              [r2-api.server.resources.discussions :as discussions]
              [r2-api.server.resources.a-discussion :as a-discussion]
              [r2-api.server.resources.messages :as messages]
              [r2-api.server.templates :as t]
              [r2-api.server.db :as db]
              [compojure.core :as c :refer [GET PUT POST DELETE]]
              [compojure.handler :as ch]
              [ring.adapter.jetty :as ra]
              [clj-time.core :refer [now]]
              [slugger.core :refer [->slug]]
              [clojure.string :refer [blank?]]
              [ring.middleware.json :refer [wrap-json-params]]
              [cheshire.core :as json]
              [clj-time.format :refer [formatters unparse]]))

(def ^:private context {:server-name "Avi’s R2"})

(c/defroutes server
  (GET "/"
    []
    (t/root context))

  (groups/create-handler context)
  (a-group/create-handler context)
  (discussions/create-handler context)
  (a-discussion/create-handler context)
  (messages/create-handler context)

  (GET "/groups/:group-id/discussions/:discussion-id/messages/:message-id"
    [group-id discussion-id message-id]
    (apply t/a-message (concat [context] (db/get-multi [group-id discussion-id message-id])))))

(def ring-handler
  "this is a var so it can be used by lein-ring"
  (-> (ch/api server)
      (wrap-json-params)
      ))

(defn -main [& args]
  (println "starting Web server")
  (ra/run-jetty ring-handler {:port 3000 :join? false}))
