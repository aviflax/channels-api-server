(ns r2-api.server.templates
  (:require [net.cgrand.enlive-html :as h]))

(defn attr-append
  "An Enlive transformer which appends to the content of the indicated attribute.
   From http://stackoverflow.com/a/12687199/7012"
  [attr f & args]
  (fn [node]
    (apply update-in node [:attrs attr] f args)))

(defn attr-content
  "An Enlive transformer which replaces the content of the indicated attribute."
  [attr content]
  (fn [node]
    (assoc-in node [:attrs attr] content)))

(h/deftemplate root "templates/root.html"
  [context]
  [:title h/any-node] (h/replace-vars context)
  [:header :h1 h/any-node] (h/replace-vars context))

(h/deftemplate groups "templates/groups.html"
  [context groups]
  [:title h/any-node] (h/replace-vars context)
  [:header :h1 h/any-node] (h/replace-vars context)
  [:ul#groups :li] (h/clone-for [group groups]
                     [:a]
                     (h/do->
                       (h/set-attr :href (str "/groups/" (:id group)))
                       (h/content (:name group)))))

(h/deftemplate a-group "templates/a_group.html"
  [context]
  [:title h/any-node] (h/replace-vars context)
  [:header :h1 h/any-node] (h/replace-vars context)
  [:a#topics] (attr-content :href (str "/groups/" (:group-id context) "/topics")))

(h/deftemplate topics "templates/topics.html"
  [context]
  [:title h/any-node] (h/replace-vars context)
  [:header :h1 h/any-node] (h/replace-vars context))

(h/deftemplate a-topic "templates/a_topic.html"
  [context]
  [:title h/any-node] (h/replace-vars context)
  [:header :h1 h/any-node] (h/replace-vars context))

(h/deftemplate messages "templates/messages.html"
  [context]
  [:title h/any-node] (h/replace-vars context)
  [:header :h1 h/any-node] (h/replace-vars context))

(h/deftemplate a-message "templates/a_message.html"
  [context]
  [:title h/any-node] (h/replace-vars context)
  [:header :h1 h/any-node] (h/replace-vars context)
  [:span.message-id] (h/content (:message-id context)))
