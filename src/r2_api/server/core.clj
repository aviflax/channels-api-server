(ns r2-api.server.core
  (:require [r2-api.server.templates :as t]
            [r2-api.server.db :as db]
            [compojure.core :as c :refer [GET PUT POST DELETE]]
            [compojure.handler :as ch]
            [ring.adapter.jetty :as ra]
            [clj-time.core :refer [now]]
            [slugger.core :refer [->slug]]
            [clojure.string :refer [blank?]]
            [com.twinql.clojure.conneg :refer [best-allowed-content-type]]
            [ring.middleware.json :refer [wrap-json-params]]
            [cheshire.core :as json]
            [clj-time.format :refer [formatters unparse]]))

(def context {:server-name "Avi’s R2"})

(def standard-content-types #{"application/json" "text/html"})

(defn select-accept-type [accept-header]
  (-> (best-allowed-content-type accept-header standard-content-types)
      second
      keyword))

(defn acceptable? [acceptable-types accept-header]
  (boolean ((set acceptable-types) (select-accept-type accept-header))))

(defn create-message-doc [group-id discussion-id body]
  {:type "message"
   :body body
   :group {:id group-id}
   :discussion {:id discussion-id}
   :created (unparse (:date-time-no-ms formatters) (now))
   :user {:id "avi-flax" :name "Avi Flax"}})

(defn group-uri [group-id] (str "/groups/" group-id))

(defn pretty-json [m] (json/generate-string m {:pretty true}))

(defn groups-to-json [groups]
  (pretty-json {:groups (map #(-> (assoc % :href (group-uri (:_id %)))
                                  (dissoc ,,, :_id))
                             groups)}))

(defn error-response [code message]
  {:status code
   :headers {"Content-Type" "text/plain;charset=UTF-8"}
   :body message})

(defn represent-groups [headers]
  (condp = (select-accept-type (get headers "accept"))
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (t/groups context (db/get-groups))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (groups-to-json (db/get-groups))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(c/defroutes server
  (GET "/"
    []
    (t/root context))

  (GET "/groups"
    {headers :headers}
    (represent-groups headers))

  (POST "/groups"
    {headers :headers {name :name} :params}
    (cond
      (and (not (.contains (get headers "content-type") "application/json"))
           (not (.contains (get headers "content-type") "application/x-www-form-urlencoded")))
      (error-response 415 "The request representation must be of the type application/json or application/x-www-form-urlencoded.")

      (or (nil? name)
          (not (string? name))
          (blank? name))
      (error-response 400 "The request must include the string parameter or property 'name', and it may not be null or blank.")

      (not= (db/get-key-count :groups name) 0)
      (error-response 409 "A group with the specified name already exists.")

      (not (acceptable? #{:html :json} (get headers "accept")))
      (error-response 406 "Not Acceptable; available content types are text/html and application/json.")

      :default
      (let [group-id (db/new-doc! {:type "group", :name name, :slug (->slug name)})]
        (-> (represent-groups headers)
            (assoc ,,, :status 201)
            (assoc-in ,,, [:headers "Location"] (group-uri group-id))))))

  (GET "/groups/:group-id"
    {params :params}
      (t/a-group (merge context params) (db/get-doc (:group-id params))))

  (GET "/groups/:group-id/discussions"
    {params :params}
    (t/discussions (merge context params)
                   (db/get-doc (:group-id params))
                   (db/get-discussions (:group-id params))))

  (POST "/groups/:group-id/discussions"
    {params :params, {:keys [group-id name]} :params}
    (let [discussion-id (db/new-doc! {:type "discussion", :name name, :slug (->slug name), :group {:id group-id}})]
      (when (and (contains? params :body)
                 (not (blank? (:body params))))
        ;; request contains body of initial message, so create that right now
        (db/new-doc! (create-message-doc group-id discussion-id (:body params))))
    (t/discussions context (db/get-doc group-id) (db/get-discussions group-id))))

  (GET "/groups/:group-id/discussions/:discussion-id"
    [group-id discussion-id]
    (t/a-discussion context (db/get-doc group-id) (db/get-doc discussion-id)))

  (GET "/groups/:group-id/discussions/:discussion-id/messages"
    [group-id discussion-id]
    (t/messages context (db/get-doc group-id) (db/get-doc discussion-id) (db/get-messages discussion-id)))

  (POST "/groups/:group-id/discussions/:discussion-id/messages"
    [group-id discussion-id body]
    (db/new-doc! (create-message-doc group-id discussion-id body))
    (t/messages context (db/get-doc group-id) (db/get-doc discussion-id) (db/get-messages discussion-id)))

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
