(ns r2-api.server.core
  (:require [r2-api.server.templates :as t]
            [r2-api.server.db :as db]
            [compojure.core :as c :refer [GET PUT POST DELETE]]
            [compojure.handler :as ch]
            [ring.adapter.jetty :as ra]))

(def context {:server-name "Avi’s R2"})

(c/defroutes server
  (GET "/"
    []
    (t/root context))

  (GET "/groups"
    []
    (t/groups context (db/get-groups)))

  (POST "/groups"
    [name]
    (db/new-doc! {:type "group" :name name})
    (t/groups context (db/get-groups)))

  (GET "/groups/:group-id"
    {params :params}
      (t/a-group (merge context params) (db/get-doc (:group-id params))))

  (GET "/groups/:group-id/topics"
    {params :params}
    (t/topics (merge context params)
              (db/get-doc (:group-id params))
              (db/get-topics (:group-id params))))

  (POST "/groups/:group-id/topics"
    [group-id name]
    (db/new-doc! {:type "topic" :name name :group {:id group-id}})
    (t/topics context (db/get-doc group-id) (db/get-topics group-id)))

  (GET "/groups/:group-id/topics/:topic-id"
    [group-id topic-id]
    (t/a-topic context (db/get-doc group-id) (db/get-doc topic-id)))

  (GET "/groups/:group-id/topics/:topic-id/messages"
    [group-id topic-id]
    (t/messages context (db/get-doc group-id) (db/get-doc topic-id) (db/get-messages topic-id)))

  (POST "/groups/:group-id/topics/:topic-id/messages"
    [group-id topic-id body]
    (db/new-doc! {:type "message"
                  :body body
                  :group {:id group-id}
                  :topic {:id topic-id}
                  :user {:id "avi-flax" :name "Avi Flax"}})
    (t/messages context (db/get-doc group-id) (db/get-doc topic-id)))

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
