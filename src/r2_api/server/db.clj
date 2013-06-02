(ns r2-api.server.db
  (:require [com.ashafa.clutch :as couch]))

(def ^:private db (couch/couch "avis-r2"))

(couch/create! db)

(defn get-doc
  "Just for convenience as it’s shorter."
  [id]
  (couch/get-document db id))

(defn new-doc!
  [doc]
  (couch/assoc! db (str (java.util.UUID/randomUUID)) doc))

(defn get-groups []
  (map #(hash-map :_id (:id %) :name (:value %))
       (couch/get-view db "api" :groups)))

(defn get-topics [group-id]
  (map #(hash-map :_id (:id %) :name (:value %))
       (couch/get-view db "api" :topics {:key group-id})))

(defn get-messages [topic-id]
  (map :doc (couch/get-view db "api" :messages {:key topic-id :include_docs "true"})))

(defn get-multi
  "Accepts a sequence of IDs and returns a map of id to document."
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
