(ns r2-api.server.resources.groups
  (:require [r2-api.server.util :refer [acceptable? error-response pretty-json select-accept-type type-supported?]]
            [compojure.core :refer [GET POST routes]]
            [r2-api.server.templates :as t]
            [r2-api.server.db :as db]
            [clj-time.core :refer [now]]
            [clj-time.format :refer [formatters unparse]]
            [slugger.core :refer [->slug]]
            [clojure.string :refer [blank?]]
            [clojure.pprint :refer :all]))

(defn group-uri [group-id] (str "/groups/" group-id))

(defn to-json [groups]
  (pretty-json {:groups (map #(-> (assoc % :href (group-uri (:_id %)))
                                  (dissoc ,,, :_id))
                             groups)}))

(def acceptable-types #{"application/json" "text/html"})

(defn represent [headers context]
  (condp = (select-accept-type acceptable-types (get headers "accept"))
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (t/groups context (db/get-groups))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (to-json (db/get-groups))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/groups"
      {headers :headers}
      (represent headers context))

    (POST "/groups"
      {headers :headers {name :name} :params}
      (cond
        (not (type-supported? ["application/json" "application/x-www-form-urlencoded"] (get headers "content-type")))
        (error-response 415 "The request representation must be of the type application/json or application/x-www-form-urlencoded.")

        (or (nil? name)
            (not (string? name))
            (blank? name))
        (error-response 400 "The request must include the string parameter or property 'name', and it may not be null or blank.")

        (not= (db/get-key-count :groups name) 0)
        (error-response 409 "A group with the specified name already exists.")

        (not (acceptable? acceptable-types (get headers "accept")))
        (error-response 406 "Not Acceptable; available content types are text/html and application/json.")

        :default
        (let [group-id (db/new-doc! (db/create-group-doc name))]
          (-> (represent headers context)
              (assoc ,,, :status 201)
              (assoc-in ,,, [:headers "Location"] (group-uri group-id))))))))
