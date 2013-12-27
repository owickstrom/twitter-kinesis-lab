(ns twitter-hashtags-visualizer.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [amazonica.aws.dynamodbv2 :as dynamo]
            [hiccup.page :refer [html5 include-css]]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as jdbc])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn pool [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(def db-spec {:classname (env :db-classname)
              :subprotocol (env :db-subprotocol)
              :subname (env :db-subname)
              :user (env :db-user)
              :password (env :db-password)})

(def pooled-db-spec (pool db-spec))

(def tags (atom {}))

(defn query-top-tags []
  {"apor" 23 "monkeys" 18 "stuff" 1})

(defn update-tags []
  (swap! tags (fn [_] (query-top-tags))))

(defn poll-db []
  (update-tags)
  (Thread/sleep 5000)
  (poll-db))

(defn start-polling []
  (println "Polling each 5 seconds")
  (.start (Thread. poll-db)))

(defn tags-template [tags]
  (let [title "Popular Twitter Tags"]
    (html5
      [:head
        [:title title]
        (include-css "main.css")]
      [:body
        [:h1 title]
        [:ul
          (for [[tag count] tags] [:li [:span tag] count])]])))

(defroutes app-routes
  (GET "/" [] (tags-template @tags))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

(defn -main [& args]
  (try
    (println)
    (println "creating table")
    (jdbc/db-do-commands
     pooled-db-spec
     (jdbc/create-table-ddl :fruit
                            [:name "varchar(32)" "PRIMARY KEY"]
                            [:price :int]))
    (println "inserting fruit"
             (jdbc/insert! pooled-db-spec :fruit {:name "apple" :price 120}))
    (println (jdbc/query pooled-db-spec ["SELECT * FROM fruit WHERE price > ? ORDER BY price" 100]))
    (println "updating price"
             (jdbc/update! pooled-db-spec :fruit {:name "apple" :price 122} ["name = ?" "apple"]))
    (println (jdbc/query pooled-db-spec ["SELECT * FROM fruit WHERE price > ? ORDER BY price" 100]))
    (println "deleting fruit"
             (jdbc/delete! pooled-db-spec :fruit ["name = ?" "apple"]))
    (println (jdbc/query pooled-db-spec ["SELECT * FROM fruit WHERE price > ? ORDER BY price" 100]))
    (finally
      (println "dropping table")
      (jdbc/db-do-commands
       pooled-db-spec
       (jdbc/drop-table-ddl :fruit)))))

(comment
  ;; for testing
  (-main)
  )
