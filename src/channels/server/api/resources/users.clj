(ns channels.server.api.resources.users
  (:require [channels.server.api.shared :refer [acceptable-types]]
            [channels.server.api.uri :as uri]
            [channels.server.api.util :refer [acceptable? error-response pretty-json resource select-accept-type type-supported?]]
            [compojure.core :refer [GET POST]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]
            [clj-time.core :refer [now]]
            [clj-time.format :refer [formatters unparse]]
            [slugger.core :refer [->slug]]
            [clojure.string :refer [blank?]]))


(defn to-json
  [context users created]
  (let [add-uri #(assoc % :href (uri/a-user (:id %)))
        m {:server {:name (:server-name context)}
           :users (map add-uri users)}
        ; TODO this seems awkward/iffy. Is there a better way to express this?
        m (if created
              (assoc m :created (add-uri created))
              m)]
    (pretty-json m)))


(h/deftemplate html-template "templates/users.html"
  [context users]
  [:html h/text-node] (h/replace-vars context)
  [:ul#users :li] (h/clone-for [user users]
                     [:a] (h/do->
                            (h/set-attr :href (uri/a-user (:id user)))
                            (h/content (:name user)))))


(defn represent
  ([accept-header context] (represent accept-header context nil))
  ([accept-header context created]
    (case (select-accept-type acceptable-types accept-header)
      :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (html-template context (db/get-users))}
      :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (to-json context (db/get-users) created)}
      (error-response 406 "Not Acceptable; available content types are text/html and application/json."))))


(defn create-resource [name email accept-header context]
  (let [created (db/create-user! name email)]
    (-> (represent accept-header context created)
        (assoc ,,, :status 201)
        (assoc-in ,,, [:headers "Location"] (uri/a-user (:id created))))))


(defn create-handler [context]
  (resource "/users"
    (GET
      {{accept-header "accept"} :headers}
      (represent accept-header context))

    (POST
      {headers :headers
       {accept-header "accept"} :headers
       {:keys [name email]} :params}
      (cond
        (not (contains? headers "content-type"))
        (error-response 400 "The request must include the header Content-Type.")

        (not (type-supported? ["application/json" "application/x-www-form-urlencoded"] (get headers "content-type")))
        (error-response 415 "The request representation must be of the type application/json or application/x-www-form-urlencoded.")

        (let [required-post-params [name email]]
          (or (some nil? required-post-params)
              (some #(not (string? %)) required-post-params)
              (some blank? required-post-params)))
        (error-response 400 "The request must include the parameters/properties 'name' and 'email', and they may not be null or blank.")

        (not (.contains email "@"))
        (error-response 400 "The supplied email address is invalid.")

        (not= (db/get-key-count :users email) 0)
        (error-response 409 "A user with the specified email address already exists.")

        (not (acceptable? acceptable-types (get headers "accept")))
        (error-response 406 "Not Acceptable; available content types are text/html and application/json.")

        :default
        (create-resource name email accept-header context)))))
