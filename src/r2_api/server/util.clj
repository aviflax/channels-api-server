(ns r2-api.server.util
  (:require [cheshire.core :as json]
            [com.twinql.clojure.conneg :refer [best-allowed-content-type]]))

(defn select-accept-type [acceptable-types accept-header]
  (-> (best-allowed-content-type accept-header acceptable-types)
      second
      keyword))

(defn acceptable? [acceptable-types accept-header]
  (boolean (select-accept-type acceptable-types accept-header)))

(defn type-supported? [supported-types content-type-header]
  (-> (some #(.contains content-type-header %) supported-types)
      boolean))

(defn pretty-json [m] (json/generate-string m {:pretty true}))

(defn error-response [code message]
  {:status code
   :headers {"Content-Type" "text/plain;charset=UTF-8"}
   :body message})
