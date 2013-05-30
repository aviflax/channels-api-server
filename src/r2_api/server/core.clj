(ns r2-api.server.core
  (:require [r2-api.server.templates :as t]
            [compojure.core :as c :refer [GET PUT POST DELETE]]
            [com.ashafa.clutch :as couch]
            [compojure.handler :as ch]
            [ring.adapter.jetty :as ra]))

(def context {:server-name "Avi’s R2"})

(def db (couch/couch "avis-r2"))

(couch/create! db)

(defn get-groups [db]
  (map #(hash-map :id (:id %) :name (:value %))
       (couch/get-view db "api" :groups)))

(c/defroutes server
  (GET "/"
    []
    (t/root context))

  (GET "/groups"
    []
    (t/groups context (get-groups db)))

  (POST "/groups"
    [name]
    (couch/assoc! db (str (java.util.UUID/randomUUID)) {:type "group" :name name})
    (t/groups context))

  (GET "/groups/:group-id"
    {params :params}
      (t/a-group (merge context params) (couch/get-document db (:group-id params))))

  (GET "/groups/:group-id/topics"
    {params :params}
    (t/topics (merge context params)))

  (GET "/groups/:group-id/topics/:topic-id"
    {params :params}
    (t/a-topic (merge context params)))

  (GET "/groups/:group-id/topics/:topic-id/messages"
    {params :params}
    (t/messages (merge context params)))

  (GET "/groups/:group-id/topics/:topic-id/messages/:message-id"
    {params :params}
    (t/a-message (merge context params))))

(def ring-handler
  "this is a var so it can be used by lein-ring"
  (-> (ch/api server)
      ;insert middleware here
      ))

(defn -main [& args]
  (println "starting Web server")
  (ra/run-jetty ring-handler {:port 3000 :join? false}))
