(ns channels.server.api.shared)

(comment "Mostly this is for stuff which can’t go in core because of cyclic dependencies.")

;; The value is specifically a vector so as to ensure that application/JSON is the default content type
(def acceptable-types ["application/json" "text/html"])
