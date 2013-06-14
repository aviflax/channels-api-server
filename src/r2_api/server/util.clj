(ns r2-api.server.util
  (:require [cheshire.core :as json]
            [com.twinql.clojure.conneg :refer [best-allowed-content-type]]))

(defn select-accept-type [acceptable-types accept-header]
  (-> (best-allowed-content-type accept-header acceptable-types)
      second
      keyword))

(def acceptable? (comp boolean select-accept-type))

(defn type-supported? [supported-types content-type-header]
  (-> (some #(.contains content-type-header %) supported-types)
      boolean))

(defn pretty-json [m] (json/generate-string m {:pretty true}))

(defn error-response [code message]
  {:status code
   :headers {"Content-Type" "text/plain;charset=UTF-8"}
   :body message})

(defn doc-to-json [m]
  (-> (assoc m :id (:_id m))
      (dissoc ,,, :_id :_rev :type)
      pretty-json))
