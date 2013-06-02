(ns r2-api.server.core
  (:require [r2-api.server.templates :as t]
            [r2-api.server.db :as db]
            [compojure.core :as c :refer [GET PUT POST DELETE]]
            [compojure.handler :as ch]
            [ring.adapter.jetty :as ra]
            [clj-time.core :refer [now]]
            [clj-time.format :refer [formatters unparse]]))

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

  (GET "/groups/:group-id/discussions"
    {params :params}
    (t/discussions (merge context params)
              (db/get-doc (:group-id params))
              (db/get-discussions (:group-id params))))

  (POST "/groups/:group-id/discussions"
    [group-id name]
    (db/new-doc! {:type "discussion" :name name :group {:id group-id}})
    (t/discussions context (db/get-doc group-id) (db/get-discussions group-id)))

  (GET "/groups/:group-id/discussions/:discussion-id"
    [group-id discussion-id]
    (t/a-discussion context (db/get-doc group-id) (db/get-doc discussion-id)))

  (GET "/groups/:group-id/discussions/:discussion-id/messages"
    [group-id discussion-id]
    (t/messages context (db/get-doc group-id) (db/get-doc discussion-id) (db/get-messages discussion-id)))

  (POST "/groups/:group-id/discussions/:discussion-id/messages"
    [group-id discussion-id body]
    (db/new-doc! {:type "message"
                  :body body
                  :group {:id group-id}
                  :discussion {:id discussion-id}
                  :created (unparse (:date-time-no-ms formatters) (now))
                  :user {:id "avi-flax" :name "Avi Flax"}})
    (t/messages context (db/get-doc group-id) (db/get-doc discussion-id) (db/get-messages discussion-id)))

  (GET "/groups/:group-id/discussions/:discussion-id/messages/:message-id"
    [group-id discussion-id message-id]
    (apply t/a-message (concat [context] (db/get-multi [group-id discussion-id message-id])))))

(def ring-handler
  "this is a var so it can be used by lein-ring"
  (-> (ch/api server)
      ;insert middleware here
      ))

(defn -main [& args]
  (println "starting Web server")
  (ra/run-jetty ring-handler {:port 3000 :join? false}))