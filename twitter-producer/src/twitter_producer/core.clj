(ns twitter-producer.core
  (:require [twitter.oauth :as oauth]
            [twitter.callbacks :as callbacks]
            [twitter.callbacks.handlers :as handlers]
            [twitter.api.streaming :as streaming]
            [cheshire.core :as json]
            [clojure.tools.logging :refer [info error]]
            [amazonica.aws.kinesis :as kinesis]
            [clj-time.format :as format]
            [clj-time.coerce :refer [to-date]]
            [environ.core :refer [env]])
  (:import twitter.callbacks.protocols.AsyncStreamingCallback
           com.amazonaws.services.kinesis.model.ResourceInUseException
           java.util.Locale)
  (:gen-class))

(def my-creds (oauth/make-oauth-creds (env :consumer-key)
                                      (env :consumer-secret)
                                      (env :access-token)
                                      (env :access-token-secret)))

(def ^:dynamic *stream-name* "Twitter")

(def ^:dynamic *kinesis-uri* "https://kinesis.us-east-1.amazonaws.com")

;; make sure we have a working stream
(defn create-stream [stream-name]
  (let [streams (kinesis/list-streams)
        stream-names (set (:stream-names streams))]
    (if (stream-names stream-name)
      (do                               ;stream exists, check status
        (info "Kinesis stream" stream-name "exists, checking status...")
        (let [stream (kinesis/describe-stream stream-name)
              status (get-in stream [:stream-description :stream-status])]
          (case status
            "ACTIVE" (info "Kinesis stream" stream-name "is active")
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
            (error "Stream" stream-name "already exists, somebody got in between")))))))

(defn on-bodypart
  "A streaming on-bodypart handler.
    baos is a ByteArrayOutputStream.  (str baos) is the response body (encoded as JSON).
    This handler will print the expanded media URL of Tweets that have media."
  [response baos]
  ;; parse the tweet (true means convert to keyword keys)
  (let [tweet (json/parse-string (str baos) true)
        ;; see https://dev.twitter.com/docs/tweet-entities
        hashtags (get-in tweet [:entities :hashtags])
        date-str (:created_at tweet)
        formatter (format/with-locale
                    (format/formatter "EEE MMM dd HH:mm:ss Z yyyy")
                    Locale/ENGLISH)
        date (when date-str
               (to-date (format/parse formatter date-str)))]
    (doseq [hashtag (mapv :text hashtags)]
      (let [data {:created-at date
                  :tag hashtag}]
        (info "Posting to Kinesis:" data)
        (kinesis/put-record *stream-name*
                            data
                            hashtag)))))

(def async-streaming-callback
  (AsyncStreamingCallback.
   ;; our handler, called for each Tweet that comes in from the Streaming API.
   on-bodypart
   ;; return the Twitter API error message on failure
   handlers/get-twitter-error-message
   ;; just print exceptions to the console when there's an exception
   handlers/exception-print))

(defn start []
    (streaming/statuses-sample :oauth-creds my-creds
                               :callbacks async-streaming-callback))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (create-stream *stream-name*)
  (start))
