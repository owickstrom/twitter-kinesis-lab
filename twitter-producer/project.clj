(defproject twitter-producer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [twitter-api "0.7.4"]
                 [cheshire "5.3.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [org.clojure/tools.logging "0.2.6"]
                 [amazonica "0.2.0"]
                 [clj-time "0.6.0"]
                 [environ "0.4.0"]]
  :main ^:skip-aot twitter-producer.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
