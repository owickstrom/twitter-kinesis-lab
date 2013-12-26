(ns twitter-hashtags-visualizer.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [amazonica.aws.dynamodbv2 :as dynamo]
            [hiccup.page :refer [html5 include-css]]))

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
