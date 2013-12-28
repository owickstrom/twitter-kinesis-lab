(defproject tagcount "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main tagcount.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [amazonica "0.2.0"]
                 [clj-time "0.6.0"]
                 [environ "0.4.0"]
                 [org.clojure/java.jdbc "0.3.0"]
                 [postgresql "9.1-901-1.jdbc4"]
                 [com.mchange/c3p0 "0.9.2.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [org.clojure/tools.logging "0.2.6"]])
