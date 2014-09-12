(defproject tagcount "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main tagcount.core
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [amazonica "0.2.24" :exclusions [commons-codec joda-time]]
                 [clj-time "0.8.0"]
                 [environ "1.0.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.7"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.clojure/tools.logging "0.2.6"]
                 [postgresql "9.3-1102.jdbc41"]
                 [com.mchange/c3p0 "0.9.2.1"]]
  :plugins [[lein-environ "1.0.0"]])
