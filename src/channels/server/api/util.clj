(ns channels.server.api.util
  (:refer-clojure :exclude [replace])
  (:require [cheshire.core :as json]
            [compojure.core :refer [ANY routes]]
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


(defmacro resource [path & methods]
  "A more concise alternative to Compojure’s `routes`. Allows the path
   to be specified only once even if a resource supports multiple methods;
   adds an ANY route to return a 405 response for any unsupported method.
   TODO: this should add an OPTIONS route and return the Allow header.

   Use like so:
   (resource path methods)

   `methods` should be standard compojure routes, except with the path omitted.

   For example:

   (resource \"/books\"
     (GET [author] (get-books author))
     (POST [title author] (create-book author) (get-books author)))
  "
  `(routes
     ~@(map (fn [[method bindings expr]]
            ;; TODO: support lists containing more than 3 forms
            `(~method ~path ~bindings ~expr))
           methods)
     (ANY ~path [] (error-response 405 "Method Not Allowed"))))


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

(defn maps-for-html
  "Combine various maps together for HTML template variable substitution"
  ([context channel]
   (merge context
          {:channel-id (:_id channel) :channel-name (:name channel)}))
  ([context channel discussion]
   (merge (maps-for-html context channel)
          {:discussion-id (:_id discussion) :discussion-subject (:subject discussion)})))

(defn indexed
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.

  (indexed '(a b c d))  =>  ([0 a] [1 b] [2 c] [3 d])

  Stolen from https://github.com/richhickey/clojure-contrib/blob/95dddbbdd748b0cc6d9c8486b8388836e6418848/src/main/clojure/clojure/contrib/seq_utils.clj#L51"
  ([s]
    (indexed s 0))

  ([s start]
    (map vector (iterate inc start) s)))