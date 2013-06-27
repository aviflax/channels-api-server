(ns r2-api.server.core
    (:require [r2-api.server.resources [root :as root]
                                       [groups :as groups]
                                       [a-group :as a-group]
                                       [discussions :as discussions]
                                       [a-discussion :as a-discussion]
                                       [messages :as messages]
                                       [a-message :as a-message]]
              [compojure [core :as c]
                         [handler :as ch]]
              [ring.adapter.jetty :as rj]
              [ring.middleware [json :refer [wrap-json-params]]
                               [head :refer [wrap-head]]]))

(def ^:private context {:server-name "Avi’s R2"})

; TODO: it appears that there are cases wherein an exception is thrown but Ring/Compojure return a 200
; with no body. The response *should* be a 500. An example case is when the CouchDB DB is missing a view.

(def routes
  ; all this so I don’t have to type `context` over and over
  (apply c/routes
         (map #(% context)
              [root/create-handler
               groups/create-handler
               a-group/create-handler
               discussions/create-handler
               a-discussion/create-handler
               messages/create-handler
               a-message/create-handler])))

(defn wrap-cors [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc response :headers
                      (assoc (:headers response)
                             "Access-Control-Allow-Origin" "*"
                             "Access-Control-Allow-Methods" "HEAD, OPTIONS, GET, PUT, POST, DELETE"
                             "Access-Control-Allow-Headers" "Content-Type")))))

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
