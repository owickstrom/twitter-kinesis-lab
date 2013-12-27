(defproject twitter-hashtags-visualizer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]]
  :plugins [[lein-ring "0.8.8"]]
  :ring {:handler twitter-hashtags-visualizer.handler/app
         :init twitter-hashtags-visualizer.handler/start-polling}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [hiccup "1.0.4"]
                        [amazonica "0.2.0"]
                        [cheshire "5.3.0"]
                        [environ "0.4.0"]
                        [org.clojure/java.jdbc "0.3.0"]
                        [postgresql "9.1-901-1.jdbc4"]
                        [com.mchange/c3p0 "0.9.2.1"]]}})
