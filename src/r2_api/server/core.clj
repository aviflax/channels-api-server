(ns r2-api.server.core
  (:require [r2-api.server.templates :as t]
            [compojure.core :as c]
            [com.ashafa.clutch :as couchdb]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]))

(def context {:server-name "Avi’s R2"})

(def db (couchdb/couch "avis-r2"))

(couchdb/create! db)

(defn get-groups [db]
  (map #(hash-map :id (:id %) :name (:value %))
       (couchdb/get-view db "api" :groups)))

(c/defroutes server
  (c/GET "/"
    []
    (t/root context))

  (c/GET "/groups"
    []
    (t/groups context (get-groups db)))

  (c/POST "/groups"
    [name]
    (couchdb/assoc! db (str (java.util.UUID/randomUUID)) {:type "group" :name name})
    (t/groups context))

  (c/GET "/groups/:group-id"
    {params :params}
    (let [group (couchdb/get-document db (:group-id params))]
      (t/a-group (merge context params {:group-id (:_id group) :group-name (:name group)}))))

  (c/GET "/groups/:group-id/topics"
    {params :params}
    (t/topics (merge context params)))

  (c/GET "/groups/:group-id/topics/:topic-id"
    {params :params}
    (t/a-topic (merge context params)))

  (c/GET "/groups/:group-id/topics/:topic-id/messages"
    {params :params}
    (t/messages (merge context params)))

  (c/GET "/groups/:group-id/topics/:topic-id/messages/:message-id"
    {params :params}
    (t/a-message (merge context params))))

(def ring-handler
  "this is a var so it can be used by lein-ring"
  (-> (api server)
      ;insert middleware here
      ))

(defn -main [& args]
  (println "starting Web server")
  (run-jetty ring-handler {:port 3000 :join? false}))
