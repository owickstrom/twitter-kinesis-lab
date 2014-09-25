(defproject twitter-producer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [twitter-streaming-client "0.3.2"
                  :exclusions [org.apache.httpcomponents/httpclient
                               org.apache.httpcomponents/httpcore]]
                 [cheshire "5.3.1"]

                 ;; twitter-streaming-client wants 1.7.5 of these, no way dude
                 [org.slf4j/slf4j-api "1.7.7"]
                 [org.slf4j/jcl-over-slf4j "1.7.7"]
                 [org.slf4j/jul-to-slf4j "1.7.7"]
                 [org.slf4j/log4j-over-slf4j "1.7.7"]

                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [amazonica "0.2.25"
                  :exclusions [commons-codec joda-time]]
                 [clj-time "0.8.0"]

                 ;; some deps want 2.2, but forget it
                 [joda-time "2.3"]

                 [environ "1.0.0"]]
  :plugins [[lein-environ "1.0.0"]]
  :main ^:skip-aot twitter-producer.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
