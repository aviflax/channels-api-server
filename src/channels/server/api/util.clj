(ns channels.server.api.util
  (:refer-clojure :exclude [replace])
  (:require [cheshire.core :as json]
            [compojure.core :refer [ANY HEAD OPTIONS routes]]
            [clojure.string :refer [blank? join replace]]
            [ring.util.response :refer [header]]
            [com.twinql.clojure.conneg :refer [best-allowed-content-type]]))

(defn select-accept-type [acceptable-types accept-header]
  (-> (best-allowed-content-type accept-header acceptable-types)
      second
      keyword))

(def acceptable? (comp boolean select-accept-type))

(defn type-supported? [supported-types content-type-header]
  (-> (some #(.contains content-type-header %) supported-types)
      boolean))

(defn pretty-json [v]
  (json/generate-string v {:pretty true
                           :key-fn #(replace (name %) \- \_)}))


(defn error-response [code message]
  {:status code
   :headers {"Content-Type" "text/plain;charset=UTF-8"}
   :body message})

(defn maps-for-html
  "Combine various maps together for HTML template variable substitution"
  ([context channel]
   (merge context
          {:channel-id (:id channel) :channel-name (:name channel)}))
  ([context channel discussion]
   (merge (maps-for-html context channel)
          {:discussion-id (:id discussion) :discussion-subject (:subject discussion)})))

(defn indexed
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.

  (indexed '(a b c d))  =>  ([0 a] [1 b] [2 c] [3 d])

  Stolen from https://github.com/richhickey/clojure-contrib/blob/95dddbbdd748b0cc6d9c8486b8388836e6418848/src/main/clojure/clojure/contrib/seq_utils.clj#L51"
  ([s]
    (indexed s 0))

  ([s start]
    (map vector (iterate inc start) s)))


(defn non-blank-string [v]
  (and (string? v)
       (not (blank? v))))


(defn contains-non-blank-string [m k]
  (and (contains? m k)
       (non-blank-string (get m k))))

