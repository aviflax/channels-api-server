(ns r2-api.server.core
    (:require [r2-api.server.resources [root :as root]
                                       [groups :as groups]
                                       [a-group :as a-group]
                                       [discussions :as discussions]
                                       [a-discussion :as a-discussion]
                                       [messages :as messages]
                                       [a-message :as a-message]]
              [compojure.core :refer [defroutes]]
              [compojure.handler :as ch]
              [ring.adapter.jetty :as rj]
              [ring.middleware.json :refer [wrap-json-params]]
              [ring.middleware.head :refer [wrap-head]]))

(def ^:private context {:server-name "Avi’s R2"})

(defroutes routes
  (root/create-handler context)
  (groups/create-handler context)
  (a-group/create-handler context)
  (discussions/create-handler context)
  (a-discussion/create-handler context)
  (messages/create-handler context)
  (a-message/create-handler context))

(defn wrap-cors [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Access-Control-Allow-Origin"] "*"))))

(def ring-handler
  "this is a var so it can be used by lein-ring"
  (-> (ch/api routes)
      wrap-cors
      wrap-json-params
      ;; TODO: there appears to be a bug in wrap-head such that Content-Length gets set to 0
      wrap-head))

(defn -main [& args]
  (println "starting Web server")
  (rj/run-jetty ring-handler {:port 3000 :join? false}))
