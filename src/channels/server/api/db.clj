(ns channels.server.api.db
  (:require [com.ashafa.clutch :as couch]
            [slugger.core :refer [->slug]]
            [clj-time.core :refer [now]]
            [clj-time.format :refer [formatters unparse]]))

(def ^:private db (couch/couch "channels-arc90-com"))

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

(defn get-channels []
  (map #(hash-map :_id (:id %) :name (:key %))
       (couch/get-view db "api" :channels {:reduce "false"})))

(defn get-discussions [channel-id]
  (map :value (couch/get-view db "api" :discussions {:key channel-id})))

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
  (-> (couch/get-view db "api" view {:key key-value :channel "true"})
      first
      :value
      (or ,,, 0)))

(defn ^:private save-doc-and-assoc-id!
   "Saves a new doc and returns the map with :_id assoced."
  [m]
  (assoc m :_id (new-doc! m)))

(defn ^:private create-channel-doc [name]
  {:type "channel"
   :name name
   :slug (->slug name)
   :created-date (unparse (:date-time-no-ms formatters) (now))
   :created-user {:id "avi-flax" :name "Avi Flax"}})

(defn ^:private create-discussion-doc [name channel-id]
  {:type "discussion"
   :name name
   :slug (->slug name)
   :channel {:id channel-id}
   :created-date (unparse (:date-time-no-ms formatters) (now))
   :created-user {:id "avi-flax" :name "Avi Flax"}})

(defn ^:private create-message-doc [channel-id discussion-id body]
  {:type "message"
   :body body
   :channel {:id channel-id}
   :discussion {:id discussion-id}
   :created (unparse (:date-time-no-ms formatters) (now))
   :user {:id "avi-flax" :name "Avi Flax"}})

(defn create-channel! [name]
  ;; This could use comp but it doesn’t because I prefer fn signatures to be specific. Also less typing.
  (save-doc-and-assoc-id! (create-channel-doc name)))

(defn create-discussion! [name channel-id]
  ;; This could use comp but it doesn’t because I prefer fn signatures to be specific. Also less typing.
  (save-doc-and-assoc-id! (create-discussion-doc name channel-id)))

(defn create-message! [channel-id discussion-id body]
  ;; This could use comp but it doesn’t because I prefer fn signatures to be specific. Also less typing.
  (save-doc-and-assoc-id! (create-message-doc channel-id discussion-id body)))
