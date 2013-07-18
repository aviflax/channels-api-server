(ns channels.server.api.uri)

(comment "Ideally each resource NS would have a `uri` function, but that doesn’t work
  because of cyclic load dependencies.")

; TODO: these could possibly be combined into a single function which would detemine
; which URI to build to which resource based on which params are passed in
; however I suspect that would make the calling code less clear

(defn channels [] "/channels")

(defn a-channel [channel-id] (str "/channels/" channel-id))

(defn discussions [channel-id] (str "/channels/" channel-id "/discussions"))

(defn a-discussion [channel-id discussion-id] (str "/channels/" channel-id "/discussions/" discussion-id))

(defn messages [channel-id discussion-id] (str "/channels/" channel-id "/discussions/" discussion-id "/messages"))

(defn a-message [channel-id discussion-id message-id] (str "/channels/" channel-id "/discussions/" discussion-id "/messages/" message-id))

(defn users [] "/users")

(defn a-user [user-id] (str "/users/" user-id))
