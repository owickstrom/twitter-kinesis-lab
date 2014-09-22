(ns twitter-producer.core
  (:require [twitter-streaming-client.core :as client]
            [twitter.oauth :as oauth]
            [twitter.api.streaming]
            [clojure.tools.logging :refer [debug info error]]
            [amazonica.aws.kinesis :as kinesis]
            [amazonica.core :refer [defcredential]]
            [clj-time.format :as format]
            [clj-time.coerce :refer [to-date]]
            [environ.core :refer [env]])
  (:import com.amazonaws.services.kinesis.model.ResourceInUseException
           java.util.Locale)
  (:gen-class))

(def twitter-creds (oauth/make-oauth-creds (env :consumer-key)
                                           (env :consumer-secret)
                                           (env :access-token)
                                           (env :access-token-secret)))

(defcredential (env :aws-access-key-id) (env :aws-secret-key) (env :aws-region))

(def ^:dynamic *kinesis-stream-name* "Twitter")

(def ^:dynamic *kinesis-uri* "https://kinesis.eu-west-1.amazonaws.com")

;; make sure we have a working stream
(defn create-kinesis-stream [stream-name]
  (let [streams (kinesis/list-streams)
        stream-names (set (:stream-names streams))]
    (if (stream-names stream-name)
      (do                               ;stream exists, check status
        (info "Kinesis stream" stream-name "exists, checking status...")
        (let [stream (kinesis/describe-stream stream-name)
              status (get-in stream [:stream-description :stream-status])]
          (case status
            "ACTIVE" (do
                       (info "Kinesis stream" stream-name "is active")
                       stream)
            "CREATING" (do (info "Stream" stream-name "is in status CREATING, waiting until it's done")
                           (Thread/sleep 5000)
                           (recur stream-name))
            "DELETING" (do (info "Stream" stream-name "is in status DELETING, will re-create when done")
                           (Thread/sleep 5000)
                           (recur stream-name)))))
      (do                               ;no stream, create it
        (info "Creating Kinesis stream" stream-name)
        (try
          (kinesis/create-stream stream-name 1)
          (catch ResourceInUseException e
            (error "Stream" stream-name "already exists, somebody got in between")
            (System/exit 2)))))))

#_(kinesis/describe-stream "Twitter")

(defn- handle-hashtag [date hashtag]
  (let [data {:created-at date
              :tag hashtag}]
    (info "Posting to Kinesis:" data)
    (kinesis/put-record *kinesis-stream-name*
                        data
                        hashtag)))

(defn- handle-tweet [tweet]
  (let [;; see https://dev.twitter.com/docs/tweet-entities
        hashtags (get-in tweet [:entities :hashtags])
        date-str (:created_at tweet)
        formatter (format/with-locale
                    (format/formatter "EEE MMM dd HH:mm:ss Z yyyy")
                    Locale/ENGLISH)
        date (when date-str
               (to-date (format/parse formatter date-str)))]

    (doseq [hashtag (mapv :text hashtags)]
      (handle-hashtag date hashtag))))

(def kinesis-stream (create-kinesis-stream *kinesis-stream-name*))

(def twitter-stream (client/create-twitter-stream
                     twitter.api.streaming/statuses-sample
                     :oauth-creds twitter-creds))

(defn cleanup []
  (info "Cancelling the Twitter stream")
  (client/cancel-twitter-stream twitter-stream)
  (info "Note that Kinesis stream" (-> kinesis-stream :stream-description :stream-arn)
        "still exists"))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (client/start-twitter-stream twitter-stream)
  (.addShutdownHook (Runtime/getRuntime) (Thread. cleanup))
  (try
    (loop []
      (let [queues (client/retrieve-queues twitter-stream)
            tweets (:tweet queues)]
        (doseq [tweet tweets]
          (debug "Handling tweet" (:id tweet))
          (handle-tweet tweet))
        (recur)))
    (catch Exception e
      (error "A problem occurred when retrieving tweets" (.getMessage e))
      (cleanup)
      (System/exit 1))))
