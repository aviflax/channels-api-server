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
  "Currently returns discussions sorted in reverse chronological order by creation date. This will eventually change
   to be sorted in reverse chronological order by the most recent message in each discussion."
  (map :value (couch/get-view db "api" :discussions {:key channel-id :descending "true"})))

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

(defn ^:private create-channel-doc [name participants access-control]
  {:type "channel"
   :name name
   :slug (->slug name)
   :participants participants
   :access-control access-control
   :created-date (unparse (:date-time-no-ms formatters) (now))
   :created-user {:id "avi-flax" :name "Avi Flax"}})

(defn ^:private create-discussion-doc [subject channel-id]
  {:type "discussion"
   :subject subject
   :slug (->slug subject)
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

(defn create-channel!
  "Creates and saves a channel and returns a map representing the channel, including :_id.
   This map will be a superset of the maps returned by `get-channels`."
  [name participants access-control]
  ;; This could use comp but it doesn’t because I prefer fn signatures to be specific. Also less typing.
  (save-doc-and-assoc-id! (create-channel-doc name participants access-control)))

(defn create-discussion!
  "Creates and saves a discussion and returns a map representing the discussion, including :_id.
   This map will be a superset of the maps returned by `get-discussions`."
  [subject channel-id]
  ;; This could use comp but it doesn’t because I prefer fn signatures to be specific. Also less typing.
  (save-doc-and-assoc-id! (create-discussion-doc subject channel-id)))

(defn create-message!
  "Creates and saves a message and returns a map representing the message, including :_id.
   This map will be a superset of the maps returned by `get-messages`."
  [channel-id discussion-id body]
  ;; This could use comp but it doesn’t because I prefer fn signatures to be specific. Also less typing.
  (save-doc-and-assoc-id! (create-message-doc channel-id discussion-id body)))
