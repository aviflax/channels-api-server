(ns r2-api.server.db
  (:require [com.ashafa.clutch :as couch]
            [slugger.core :refer [->slug]]
            [clj-time.core :refer [now]]
            [clj-time.format :refer [formatters unparse]]))

(def ^:private db (couch/couch "avis-r2"))

(couch/create! db)

(defn get-doc
  "Just for convenience as it’s shorter."
  [id]
  (couch/get-document db id))

(defn new-doc!
  [doc]
  (let [id (str (java.util.UUID/randomUUID))]
    (couch/assoc! db id doc)
    id))

(defn get-groups []
  (map #(hash-map :_id (:id %) :name (:key %))
       (couch/get-view db "api" :groups {:reduce "false"})))

(defn get-discussions [group-id]
  (map :value (couch/get-view db "api" :discussions {:key group-id})))

(defn get-messages [discussion-id]
  (map :doc (couch/get-view db "api" :messages {:startkey [discussion-id] :endkey [discussion-id {}] :include_docs "true"})))

(defn get-multi
  "Accepts a sequence of IDs and returns a sequence of retrieved documents (as maps) in the same order as the provided IDs."
  [ids]
  (map :doc
       (couch/all-documents db {:include_docs true} {:keys ids})))

(defn get-multi-map
  "Accepts a sequence of IDs and returns a map of id to document."
  [ids]
  (->> (get-multi ids)
       (map #(vector (:_id %) %))
       flatten
       (apply hash-map)))

(defn get-key-count
  [view key-value]
  (-> (couch/get-view db "api" view {:key key-value :group "true"})
      first
      :value
      (or ,,, 0)))

(defn create-group-doc [name]
  {:type "group"
   :name name
   :slug (->slug name)
   :created-date (unparse (:date-time-no-ms formatters) (now))
   :created-user {:id "avi-flax" :name "Avi Flax"}})

(defn create-discussion-doc [name group-id]
  {:type "discussion"
   :name name
   :slug (->slug name)
   :group {:id group-id}
   :created-date (unparse (:date-time-no-ms formatters) (now))
   :created-user {:id "avi-flax" :name "Avi Flax"}})

(defn create-message-doc [group-id discussion-id body]
  {:type "message"
   :body body
   :group {:id group-id}
   :discussion {:id discussion-id}
   :created (unparse (:date-time-no-ms formatters) (now))
   :user {:id "avi-flax" :name "Avi Flax"}})
