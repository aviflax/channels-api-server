(ns r2-api.server.util
  (:refer-clojure :exclude [replace])
  (:require [cheshire.core :as json]
            [clojure.string :refer [replace]]
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

(defn doc-for-json
  "Prepare a doc map for representation as JSON"
  [m]
  (-> (assoc m :id (:_id m))
      (dissoc ,,, :_id :_rev :type)))

(def doc-to-json (comp pretty-json doc-for-json))

(defn attr-append
  "An Enlive transformer which appends to the content of the indicated attribute.
   From http://stackoverflow.com/a/12687199/7012"
  [attr f & args]
  (fn [node]
    (apply update-in node [:attrs attr] f args)))

(defn combine
  ([context group]
   (merge context
          {:group-id (:_id group) :group-name (:name group)}))
  ([context group discussion]
   (merge (combine context group)
          {:discussion-id (:_id discussion) :discussion-name (:name discussion)})))

(defn indexed
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.

  (indexed '(a b c d))  =>  ([0 a] [1 b] [2 c] [3 d])

  Stolen from https://github.com/richhickey/clojure-contrib/blob/95dddbbdd748b0cc6d9c8486b8388836e6418848/src/main/clojure/clojure/contrib/seq_utils.clj#L51"
  ([s]
    (indexed s 0))

  ([s start]
    (map vector (iterate inc start) s)))