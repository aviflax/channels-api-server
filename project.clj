(defproject channels-api-server "0.1.0-SNAPSHOT"
  :description "API Server for Channels, hosting the API and mail servers"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [resourceful "0.1.1"] ;includes ring-core and compojure
                 [ring/ring-jetty-adapter "1.2.1"]
                 [ring/ring-json "0.2.0" :exclusions [cheshire]]
                 [cheshire "5.2.0"]
                 [com.ashafa/clutch "0.4.0-RC1" :exclusions [cheshire]]
                 [enlive/enlive "1.1.5"]
                 [slugger "1.0.1"]
                 [com.twinql.clojure/clj-conneg "1.1.0"]
                 [clj-time "0.6.0"]]
  :dev-dependencies []
  :main channels.server.api.core
  :plugins [[lein-ring "0.8.8"]]
  :ring {:handler channels.server.api.core/ring-handler})
