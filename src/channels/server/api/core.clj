(ns channels.server.api.core
    (:require [channels.server.api.resources [root :as root]
                                       [channels :as channels]
                                       [a-channel :as a-channel]
                                       [discussions :as discussions]
                                       [a-discussion :as a-discussion]
                                       [messages :as messages]
                                       [a-message :as a-message]]
              [compojure [core :as c]
                         [handler :as ch]
                         [route :refer [not-found]]]
              [ring.adapter.jetty :as rj]
              [ring.middleware [json :refer [wrap-json-params]]
                               [head :refer [wrap-head]]]))

(def ^:private context {:server-name "channels.arc90.com"})

; TODO: it appears that there are cases wherein an exception is thrown but Ring/Compojure return a 200
; with no body. The response *should* be a 500. An example case is when the CouchDB DB is missing a view.

(def routes
  ; all this so I don’t have to type `context` over and over
  (apply c/routes
         (-> (map #(% context)
                  [root/create-handler
                   channels/create-handler
                   a-channel/create-handler
                   discussions/create-handler
                   a-discussion/create-handler
                   messages/create-handler
                   a-message/create-handler])
             (concat ,,, [(not-found "No resource with the specified URI exists.")]))))

(defn wrap-cors [handler]
  (fn [request]
    (let [response (handler request)
          response (assoc response :headers
                          (assoc (:headers response)
                                 "Access-Control-Allow-Origin" "*"
                                 "Access-Control-Allow-Methods" "OPTIONS, HEAD, GET, PUT, POST, DELETE"
                                 "Access-Control-Allow-Headers" "Content-Type"
                                 "Access-Control-Expose-Headers" "Location"
                                 "Access-Control-Max-Age" "3600"))]
      ;; if the request is an OPTIONS request and the response is a 404, it’s likely that there was simply
      ;; no route established for OPTIONS and the request path; so we’ll just assume this is a CORS preflight
      ;; request and respond with a 200 and an empty body so as to enable CORS functionality
      (if (and (= (:request-method request) :options)
               (= (:status response) 404))
          (assoc response :status 200, :body "")
          response))))

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
