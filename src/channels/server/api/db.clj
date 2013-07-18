(ns channels.server.api.db
  (:require [clojure.string :refer [blank?]]
            [com.ashafa.clutch :as couch]
            [slugger.core :refer [->slug]]
            [clj-time.core :refer [now]]
            [clj-time.format :refer [formatters unparse]]))

(comment "Data Access Adapter for the Channels server. Currently users CouchDB, but
  *attempts* to insulate callers from CouchDB, to some degree.")

(def ^:private db (couch/couch "channels-arc90-com"))

(couch/create! db)

(defn ^:private cleanup-doc
  "Removes couchdb-specific aspects from a doc"
  [doc]
  (-> (assoc doc :id (:_id doc))
      (dissoc ,,, :_id :_rev :type)))

(defn get-doc
  "Gets a doc from the DB by ID.
   Copies :_id to :id, and removes :_id, :_rev, and :type.
   Returns nil if a doc with the specified ID doesn’t exist"
  [id]
  (when-let [doc (couch/get-document db id)]
    (cleanup-doc doc)))

(defn new-doc!
  [doc]
  (let [id (str (java.util.UUID/randomUUID))]
    (couch/assoc! db id doc)
    id))


(defn ^:private get-view [view]
  (map #(-> (assoc % :name (:key %))
            (dissoc ,,, :key :value))
        (couch/get-view db "api" (keyword view) {:reduce "false"})))


(defn get-channels [] (get-view :channels))

(defn get-users [] (get-view :users))

(defn get-discussions [channel-id]
  "Currently returns discussions sorted in reverse chronological order by creation date. This will eventually change
   to be sorted in reverse chronological order by the most recent message in each discussion."
  (map :value (couch/get-view db "api" :discussions {:key channel-id :descending "true"})))


(defn get-messages [discussion-id]
  (map (comp cleanup-doc :doc)
       (couch/get-view db "api" :messages {:startkey [discussion-id] :endkey [discussion-id {}] :include_docs "true"})))


(defn get-multi
  "Accepts a sequence of IDs and returns a lazy sequence of retrieved documents (as maps) in the same order as the provided IDs."
  [ids]
  (map (comp cleanup-doc :doc)
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
  (-> (couch/get-view db "api" view {:key key-value})
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
   :slug (when (and (string? name)
                    (not (blank? name)))
               (->slug name)) ;; TODO: if name is null or blank, we really do still need a slug. Maybe base it on the recipients? Or... random words? TBD.
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

(defn ^:private create-message-doc [channel-id discussion-id user body]
  {:type "message"
   :body body
   :channel {:id channel-id}
   :discussion {:id discussion-id}
   :created (unparse (:date-time-no-ms formatters) (now))
   :user user})


(defn ^:private create-user-doc [name email]
  {:type "user"
   :name name
   :email email
   :slug (->slug name)
   :created-date (unparse (:date-time-no-ms formatters) (now))})


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
  [channel-id discussion-id user body]
  ;; This could use comp but it doesn’t because I prefer fn signatures to be specific. Also less typing.
  (save-doc-and-assoc-id! (create-message-doc channel-id discussion-id user body)))

(defn create-user!
  "Creates and saves a user and returns a map representing the user, including :_id.
   This map will be a superset of the maps returned by `get-users`."
  [name email]
  ;; This could use comp but it doesn’t because I prefer fn signatures to be specific. Also less typing.
  (save-doc-and-assoc-id! (create-user-doc name email)))
