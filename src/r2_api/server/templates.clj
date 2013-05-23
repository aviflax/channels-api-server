(ns r2-api.server.templates
  (:require [net.cgrand.enlive-html :as h]))

(h/deftemplate root "templates/root.html"
  [context]
  [:title h/any-node] (h/replace-vars context)
  [:header :h1 h/any-node] (h/replace-vars context))

(h/deftemplate groups "templates/groups.html"
  [context groups]
  [:title h/any-node] (h/replace-vars context)
  [:header :h1 h/any-node] (h/replace-vars context)
  [:ul#groups :li] (h/clone-for [group groups]
                     (h/content (:name group))))

(h/deftemplate a-group "templates/a_group.html"
  [context]
  [:title h/any-node] (h/replace-vars context)
  [:header :h1 h/any-node] (h/replace-vars context))

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
