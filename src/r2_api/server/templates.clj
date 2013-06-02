(ns r2-api.server.templates
  (:require [net.cgrand.enlive-html :as h]))

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
  ([context group topic]
   (merge (combine context group)
          {:topic-id (:_id topic) :topic-name (:name topic)})))

(defn ^:private indexed
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.

  (indexed '(a b c d))  =>  ([0 a] [1 b] [2 c] [3 d])

  Stolen from https://github.com/richhickey/clojure-contrib/blob/95dddbbdd748b0cc6d9c8486b8388836e6418848/src/main/clojure/clojure/contrib/seq_utils.clj#L51"
  ([s]
    (indexed s 0))

  ([s start]
    (map vector (iterate inc start) s)))

(h/deftemplate root "templates/root.html"
  [context]
  [:html h/text-node] (h/replace-vars context))

(h/deftemplate groups "templates/groups.html"
  [context groups]
  [:html h/text-node] (h/replace-vars context)
  [:ul#groups :li] (h/clone-for [group groups]
                     [:a] (h/do->
                            (h/set-attr :href (str "/groups/" (:_id group)))
                            (h/content (:name group)))))

(h/deftemplate a-group "templates/a_group.html"
  [context group]
  [:html h/text-node] (h/replace-vars (combine context group))
  [:a#topics] (h/set-attr :href (str "/groups/" (:_id group) "/topics")))

(h/deftemplate topics "templates/topics.html"
  [context group topics]
  [:html h/text-node] (h/replace-vars (combine context group))
  [:ul#topics :li] (h/clone-for [topic topics]
                     [:a] (h/do->
                            (h/set-attr :href (str "/groups/" (:_id group) "/topics/" (:_id topic)))
                            (h/content (:name topic))))
  [:a#group] (attr-append :href str (:_id group))
  [:input#group-id] (h/set-attr :value (:_id group)))

(h/deftemplate a-topic "templates/a_topic.html"
  [context group topic]
  [:html h/text-node] (h/replace-vars (assoc (combine context group topic)
                                             ;; TODO: TEMP HARD-CODED VALUE
                                             :message-count "2"))
  [:a#group] (attr-append :href str (:_id group))
  ;; TODO: instead of building the URLs entirely here in the code, it’d be better to have a version of
  ;; replace-vars which can replace vars in an attribute.
  [:a#topics] (h/set-attr :href (str "/groups/" (:_id group) "/topics"))
  [:a#messages] (h/set-attr :href (str "/groups/" (:_id group) "/topics/" (:_id topic) "/messages")))

(h/deftemplate messages "templates/messages.html"
  [context group topic messages]
  [:html h/text-node] (h/replace-vars (combine context group topic))
  [:a#group] (attr-append :href str (:_id group))
  [:a#topics] (h/set-attr :href (str "/groups/" (:_id group) "/topics"))
  [:a#topic] (h/set-attr :href (str "/groups/" (:_id group) "/topics/" (:_id topic)))
  [:input#group-id] (h/set-attr :value (:_id group))
  [:input#topic-id] (h/set-attr :value (:_id topic))
  [:article.message] (h/clone-for [[i message] (indexed messages 1)]
                       [:a#user] (h/do->
                                   (h/set-attr :href (str "/people/" (get-in message [:user :id])))
                                   (h/content (get-in message [:user :name])))
                       [:pre] (h/content (:body message))
                       [:#date] (h/content (:created message))
                       [:#message-number] (h/content (str i))
                       [:a#message] (h/set-attr :href (str "/groups/" (:_id group) "/topics/" (:_id topic) "/messages/" (:_id message)))))

(h/deftemplate a-message "templates/a_message.html"
  [context group topic message]
  [:html h/text-node] (h/replace-vars (combine context group topic))
  [:span.message-id] (h/content (:message-id context))
  [:a#group] (attr-append :href str (:_id group))
  [:a#topics] (h/set-attr :href (str "/groups/" (:_id group) "/topics"))
  [:a#topic] (h/set-attr :href (str "/groups/" (:_id group) "/topics/" (:_id topic)))
  [:a#messages] (h/set-attr :href (str "/groups/" (:_id group) "/topics/" (:_id topic) "/messages"))
  [:article :pre] (h/content (:body message))
  [:#username] (h/content (get-in message [:user :name]))
  [:a#user] (h/do->
              (h/set-attr :href (str "/people/" (get-in message [:user :id])))
              (h/content (get-in message [:user :name])))
  [:.message-date] (h/content (:created message)))
