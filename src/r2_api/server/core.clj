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
  (map #(hash-map :_id (:id %) :name (:value %))
       (couch/get-view db "api" :groups)))

(defn get-doc
  "Just for convenience as it’s shorter."
  [db id]
  (couch/get-document db id))

(defn get-topics [db group-id]
  (map #(hash-map :_id (:id %) :name (:value %))
       (couch/get-view db "api" :topics {:key group-id})))

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
    (t/groups context (get-groups db)))

  (GET "/groups/:group-id"
    {params :params}
      (t/a-group (merge context params) (get-doc db (:group-id params))))

  (GET "/groups/:group-id/topics"
    {params :params}
    (t/topics (merge context params)
              (couch/get-document db (:group-id params))
              (get-topics db (:group-id params))))

  (POST "/groups/:group-id/topics"
    [group-id name]
    (couch/assoc! db (str (java.util.UUID/randomUUID)) {:type "topic" :name name :group {:id group-id}})
    (t/topics context (get-doc db group-id) (get-topics db group-id)))

  (GET "/groups/:group-id/topics/:topic-id"
    [group-id topic-id]
    (t/a-topic context (get-doc db group-id) (get-doc db topic-id)))

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
