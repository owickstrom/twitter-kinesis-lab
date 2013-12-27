(ns twitter-hashtags-visualizer.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [cheshire.core :as json]
            [amazonica.aws.dynamodbv2 :as dynamo]
            [hiccup.page :refer [html5 include-css include-js]]
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
  (take 10 (jdbc/query pooled-db-spec
                       ["
SELECT * FROM tag_count
 WHERE valid_to > CURRENT_TIMESTAMP - INTERVAL '75 minutes'
 ORDER BY count DESC
 LIMIT 10"]
                       :row-fn #(select-keys % [:tag :count]))))

(defn update-tags []
  (reset! tags (query-top-tags)))

(defn poll-db []
  (update-tags)
  (Thread/sleep 5000)
  (poll-db))

(defn start-polling []
  (println "Polling each 5 seconds")
  (.start (Thread. poll-db)))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"
             "Cache-Control" "public, max-age=0, nocache"}
   :body (json/generate-string data)})

(defn page []
  (let [title "Popular Twitter Tags"]
    (html5
      [:head
        [:title title]
        (include-css "main.css")
        (include-js "http://code.jquery.com/jquery-2.0.3.min.js"
                    "http://underscorejs.org/underscore.js"
                    "tags.js")]
      [:body
        [:h1 title]
        [:div {:class "tags-container"}]])))

(defroutes app-routes
  (GET "/" [] (page))
  (GET "/tags" [] (json-response @tags))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
