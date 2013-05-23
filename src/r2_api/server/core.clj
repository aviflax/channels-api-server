(ns r2-api.server.core
  (:require [r2-api.server.templates :as t]
            [compojure.core :as c]
            [compojure.handler :refer [api]]
            [ring.adapter.jetty :refer [run-jetty]]))

(def context {:server-name "Avi’s A2"})

(c/defroutes server
  (c/GET "/"
    []
    (t/root context))

  (c/GET "/groups"
    []
    (t/groups context))

  (c/GET "/groups/:group-id"
    []
    (t/a-group context))

  (c/GET "/groups/:group-id/topics"
    []
    (t/topics context))

  (c/GET "/groups/:group-id/topics/:topic-id"
    []
    (t/a-topic context))

  (c/GET "/groups/:group-id/topics/:topic-id/messages"
    []
    (t/messages context))

  (c/GET "/groups/:group-id/topics/:topic-id/messages/:message-id"
    [message-id]
    (t/a-message (assoc context :message-id message-id))))

(def ring-handler
  "this is a var so it can be used by lein-ring"
  (-> (api server)
      ;insert middleware here
      ))

(defn -main [& args]
  (println "starting Web server")
  (run-jetty ring-handler {:port 3000 :join? false}))
