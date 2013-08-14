(ns channels.server.api.util
  (:refer-clojure :exclude [replace])
  (:require [cheshire.core :as json]
            [compojure.core :refer [ANY OPTIONS routes]]
            [clojure.string :refer [blank? join replace]]
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


(defmacro resource [path & body]
  "Provides more concise alternative to Compojure’s `routes`, with these features:

   * Allows the path to be specified only once even if a resource supports multiple methods
   * Adds an OPTIONS route which returns an Allow header
   * Adds an ANY route to return a 405 response for any unsupported method

   Use like so:
   (resource path method method)

   `methods` should be standard compojure routes, except with the path omitted.

   For example:

   (resource \"/books\"
     (GET [author] (get-books author))
     (POST [title author] (create-book author) (get-books author)))
  "
  `(routes
     ~@(map (fn [[method bindings & exprs]]
              `(~method ~path ~bindings ~@exprs))
            body)

     ~@(let [methods (set (map first body))
             allowed (->> (map name methods)
                          (concat ["OPTIONS" "HEAD"] ,,,)
                          (join ", " ,,,))]
         `(
           ~(when-not (methods 'OPTIONS)
             (let []
               `(OPTIONS ~path [] {:status 200
                                   :headers {"Allow" ~allowed}
                                   :body nil})))
           ~(when-not (methods 'ANY)
             `(ANY ~path [] {:status 405
                             :headers {"Allow" ~allowed
                                       "Content-Type" "text/plain;charset=UTF-8"}
                             :body "Method Not Allowed"}))))))


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

