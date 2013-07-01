(ns channels.server.api.resources.root
  (:require [channels.server.api.util :refer [error-response pretty-json select-accept-type]]
            [compojure.core :refer [GET routes]]
            [net.cgrand.enlive-html :as h]))

(def acceptable-types #{"application/json" "text/html"})

(def links [{:href "groups", :text "Groups"}
            {:href "", :text "People (coming soon)"}
            {:href "", :text "Webhooks (coming soon)"}])

(h/deftemplate html-template "templates/root.html"
  [context links]
  [:html h/text-node] (h/replace-vars context)
  [:nav :ul [:li (h/but h/first-child)]] :remove
  [:nav :ul :li] (h/clone-for [link links]
                              [:a] (h/do-> (h/set-attr :href (:href link))
                                           (h/content (:text link)))))

(defn to-json [context links]
  (pretty-json {:server {:name (:server-name context)}
                :links links}))

(defn represent [accept-header context links]
  (condp = (select-accept-type acceptable-types accept-header)
    :html {:headers {"Content-Type" "text/html;charset=UTF-8"} :body (html-template context links)}
    :json {:headers {"Content-Type" "application/json;charset=UTF-8"} :body (to-json context links)}
    (error-response 406 "Not Acceptable; available content types are text/html and application/json.")))

(defn create-handler [context]
  (routes
    (GET "/" {{accept-header "accept"} :headers}
      (represent accept-header context links))))
