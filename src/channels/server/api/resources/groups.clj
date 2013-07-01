(ns channels.server.api.resources.groups
  (:require [channels.server.api.resources.a-group :refer [uri]]
            [channels.server.api.util :refer [acceptable? error-response pretty-json select-accept-type type-supported?]]
            [compojure.core :refer [GET POST routes]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]
            [clj-time.core :refer [now]]
            [clj-time.format :refer [formatters unparse]]
            [slugger.core :refer [->slug]]
            [clojure.string :refer [blank?]]
            [clojure.pprint :refer :all]))

(defn to-json [context groups]
  (pretty-json {:server {:name (:server-name context)}
                :groups (map #(-> (assoc % :href (uri (:_id %)))
                                  (assoc ,,, :id (:_id %))
                                  (dissoc ,,, :_id :_rev :type))
                             groups)}))

(def acceptable-types #{"application/json" "text/html"})

(h/deftemplate html-template "templates/groups.html"
  [context groups]
  [:html h/text-node] (h/replace-vars context)
  [:ul#groups :li] (h/clone-for [group groups]
                     [:a] (h/do->
                            (h/set-attr :href (uri (:_id group)))
                            (h/content (:name group)))))

(defn represent [headers context]
  (case (select-accept-type acceptable-types (get headers "accept"))
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (html-template context (db/get-groups))}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (to-json context (db/get-groups))}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (let [path "/groups"]
    (routes
      (GET path
        {headers :headers}
        (represent headers context))

      (POST path
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
                (assoc-in ,,, [:headers "Location"] (uri group-id)))))))))
