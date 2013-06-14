(ns r2-api.server.resources.discussions
  (:require [r2-api.server.util :refer [acceptable? error-response pretty-json select-accept-type type-supported?]]
            [compojure.core :refer [GET POST routes]]
            [r2-api.server.templates :as t]
            [r2-api.server.db :as db]
            [clojure.string :refer [blank?]]
            [clojure.pprint :refer :all]))

(defn uri [group-id discussion-id] (str "/groups/" group-id "/discussions/" discussion-id))

(defn to-json [discussions]
  (pretty-json {:discussions (map #(-> (assoc % :href (uri (get-in % [:group :id]) (:_id %)))
                                       (dissoc ,,, :_id :_rev :type))
                                  discussions)}))

(def acceptable-types #{"application/json" "text/html"})

(defn represent [accept-header group-id context]
  (condp = (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (t/discussions context (db/get-doc group-id)
                                                                                            (db/get-discussions group-id))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (to-json (db/get-discussions group-id))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/groups/:group-id/discussions"
      {{group-id :group-id} :params
       {accept-header "accept"} :headers}
      (represent accept-header group-id context))

    (POST "/groups/:group-id/discussions"
      {headers :headers
       {:keys [group-id name] :as params} :params}
      (cond
        (not (type-supported? ["application/json" "application/x-www-form-urlencoded"] (get headers "content-type")))
        (error-response 415 "The request representation must be of the type application/json or application/x-www-form-urlencoded.")

        (or (nil? name)
            (not (string? name))
            (blank? name))
        (error-response 400 "The request must include the string parameter or property 'name', and it may not be null or blank.")

        (not (acceptable? acceptable-types (get headers "accept")))
        (error-response 406 "Not Acceptable; available content types are text/html and application/json.")

        :default
        (let [discussion-id (db/new-doc! (db/create-discussion-doc name group-id))]
          (when (and (contains? params :body)
                     (string? (:body params))
                     (not (blank? (:body params))))
            ;; request contains body of initial message, so create that right now
            (db/new-doc! (db/create-message-doc group-id discussion-id (:body params))))
          (-> (represent (get headers "accept") group-id context)
              (assoc ,,, :status 201)
              (assoc-in ,,, [:headers "Location"] (uri group-id discussion-id))))))))
