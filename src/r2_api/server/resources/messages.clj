(ns r2-api.server.resources.messages
  (:require [r2-api.server.util :refer [acceptable? error-response pretty-json select-accept-type type-supported?]]
            [compojure.core :refer [GET POST routes]]
            [r2-api.server.templates :as t]
            [r2-api.server.db :as db]
            [clojure.string :refer [blank?]]
            [clojure.pprint :refer :all]))

(defn uri [group-id discussion-id message-id] (str "/groups/" group-id "/discussions/" discussion-id "/messages/" message-id))

(defn to-json [messages]
  (pretty-json {:messages (map #(-> (assoc % :href (uri (get-in % [:group :id]) (get-in % [:discussion :id]) (:_id %)))
                                    (dissoc ,,, :_id :_rev))
                               messages)}))

(def acceptable-types #{"application/json" "text/html"})

(defn represent [accept-header group-id discussion-id context]
  (condp = (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (t/messages context (db/get-doc group-id)
                                                                                         (db/get-doc discussion-id)
                                                                                         (db/get-messages discussion-id))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (to-json (db/get-messages discussion-id))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/groups/:group-id/discussions/:discussion-id/messages"
      {{:keys [group-id discussion-id]} :params
       {accept-header "accept"} :headers}
      (represent accept-header group-id discussion-id context))

    (POST "/groups/:group-id/discussions/:discussion-id/messages"
      {headers :headers
       {:keys [group-id discussion-id body]} :params}
      (cond
        (not (type-supported? ["application/json" "application/x-www-form-urlencoded"] (get headers "content-type")))
        (error-response 415 "The request representation must be of the type application/json or application/x-www-form-urlencoded.")

        (or (nil? body)
            (not (string? body))
            (blank? body))
        (error-response 400 "The request must include the string parameter or property 'body', and it may not be null or blank.")

        (not (acceptable? acceptable-types (get headers "accept")))
        (error-response 406 "Not Acceptable; available content types are text/html and application/json.")

        :default
        (let [message-id (db/new-doc! (db/create-message-doc group-id discussion-id body))]
          (-> (represent (get headers "accept") group-id discussion-id context)
              (assoc ,,, :status 201)
              (assoc-in ,,, [:headers "Location"] (uri group-id discussion-id message-id))))))))
