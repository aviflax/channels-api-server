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


(defn body-length [body]
  ;; TODO: is this treating character encodings correctly?
  (condp instance? body
    String (count body)
    java.io.File (.length body)
    java.io.InputStream (.available body)
    clojure.lang.ISeq (count (join body))
    nil))


(defmacro resource
  "Provides more concise and more RESTful alternative to Compojure’s `routes`.

  Specifically:

   * Allows the path to be specified only once even if a resource supports multiple methods
   * Adds a HEAD route, if GET is specified and HEAD is not
   * Adds an OPTIONS route which returns an Allow header, if OPTIONS is not specified
   * Adds an ANY route to return a 405 response for any unsupported method

   `methods` should be 1–N standard compojure route forms, except with the path omitted.

   Expands into a call to `routes`, so can be used anywhere `routes` can be used.

   For example:

   (resource \"Collection of the books of an author\"
             \"/authors/:author/books\"
             (GET [author] (get-books author))
             (POST [author title] (create-book author) (get-books author)))"
  [name path & methods]
  (let [method-symbols (set (map first methods))
        allowed (->> (map str method-symbols)
                     (concat ["OPTIONS" (when (or (method-symbols 'HEAD)
                                                  (method-symbols 'GET))
                                              "HEAD")] ,,,)
                     (filter (complement nil?) ,,,)
                     (join ", " ,,,))]
    `(routes
      ;; Building a list “manually” using concat (as opposed to just unquote-splicing)
      ;; because the “when” forms can produce nil values which must be filtered out of the list
      ~@(-> [
            ;; add a HEAD route if GET is provided and HEAD is not
            ;; this MUST come before the provided methods/routes, because Compojure’s GET
            ;; route also handles HEAD requests (and has a bug; it sends Content-Length as 0)
            (when (and (method-symbols 'GET)
                        (not (method-symbols 'HEAD)))
               (let [get-method (-> (filter #(= (first %) 'GET) methods)
                                    first)
                     [_ bindings & exprs] get-method]
                 `(HEAD ~path ~bindings
                    (let [get-response# (do ~@exprs)
                          response# (dissoc get-response# :body)]
                      (if (get-in response# [:headers "Content-Length"])
                          response#
                          ;; TODO: how to handle case where body-length could not determine a length?
                          (header response# "Content-Length" (body-length
                                                               (:body get-response#))))))))
            ]

            (concat ,,,
                    ;; output the provided methods/routes
                    (map (fn [[method-symbol bindings & exprs]]
                             `(~method-symbol ~path ~bindings ~@exprs))
                           methods))

            (concat ,,, [
                    ;; output OPTIONS, if it isn’t already provided
                    (when-not (method-symbols 'OPTIONS)
                      `(OPTIONS ~path [] {:status 204
                                          :headers {"Allow" ~allowed}
                                          :body nil}))

                    ;; output an ANY route to return a 405 for any unsupported method
                    (when-not (method-symbols 'ANY)
                      `(ANY ~path [] {:status 405
                                      :headers {"Allow" ~allowed
                                                "Content-Type" "text/plain;charset=UTF-8"}
                                      :body "Method Not Allowed"}))
            ])
            (->> (filter (complement nil?) ,,,))))))


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

