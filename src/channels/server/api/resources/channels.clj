(ns channels.server.api.resources.channels
  (:require [channels.server.api.resources.a-channel :refer [uri]]
            [channels.server.api.util :refer [acceptable? error-response pretty-json select-accept-type type-supported?]]
            [compojure.core :refer [GET POST routes]]
            [net.cgrand.enlive-html :as h]
            [channels.server.api.db :as db]
            [clj-time.core :refer [now]]
            [clj-time.format :refer [formatters unparse]]
            [slugger.core :refer [->slug]]
            [clojure.string :refer [blank?]]
            [clojure.pprint :refer :all]))

(defn to-json
  ([context channels] (to-json context channels nil))
  ([context channels created]
  (let [massage-channel #(-> (assoc % :id (:_id %), :href (uri (:_id %)))
                             (dissoc ,,, :_id :_rev :type))
        m {:server {:name (:server-name context)}
           :channels (map massage-channel channels)}
        ; TODO this seems awkward/iffy. Is there a better way to express this?
        m (if created
              (assoc m :created (massage-channel created))
              m)]
    (pretty-json m))))

(def acceptable-types #{"application/json" "text/html"})

(h/deftemplate html-template "templates/channels.html"
  [context channels]
  [:html h/text-node] (h/replace-vars context)
  [:ul#channels :li] (h/clone-for [channel channels]
                     [:a] (h/do->
                            (h/set-attr :href (uri (:_id channel)))
                            (h/content (:name channel)))))

(defn represent
  ([accept-header context] (represent accept-header context nil))
  ([accept-header context created]
    (case (select-accept-type acceptable-types accept-header)
      :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (html-template context (db/get-channels))}
      :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (to-json context (db/get-channels) created)}
      (error-response 406 "Not Acceptable; available content types are text/html and application/json."))))

(defn create-handler [context]
  (let [path "/channels"]
    (routes
      (GET path
        {headers :headers}
        (represent (get headers "accept") context))

      (POST path
        {headers :headers {:keys [name participants access_control]} :params}
        (cond
          (not (type-supported? ["application/json" "application/x-www-form-urlencoded"] (get headers "content-type")))
          (error-response 415 "The request representation must be of the type application/json or application/x-www-form-urlencoded.")

          (let [required-post-params [participants access_control]] ; `name` is optional
            (or (some nil? required-post-params)
                (some #(not (string? %)) required-post-params)
                (some blank? required-post-params)))
          (error-response 400 "The request must include the parameters/properties 'participants' and 'access_control', and they may not be null or blank.")

          ;; TODO: Validate participants

          (not (#{"all-users" "participants-and-admins"} access_control))
          (error-response 400 "The value of the parameter/property 'access_control' must be either 'all-users' or 'participants-and-admins'.")

          (not= (db/get-key-count :channels name) 0)
          (error-response 409 "A channel with the specified name already exists.")

          (not (acceptable? acceptable-types (get headers "accept")))
          (error-response 406 "Not Acceptable; available content types are text/html and application/json.")

          :default
          (let [created (db/create-channel! name participants access_control)]
            (-> (represent (get headers "accept") context created)
                (assoc ,,, :status 201)
                (assoc-in ,,, [:headers "Location"] (uri (:_id created))))))))))
