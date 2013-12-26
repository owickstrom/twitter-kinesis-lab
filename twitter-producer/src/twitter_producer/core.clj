(ns twitter-producer.core
  (:require [twitter.oauth :as oauth]
            [twitter.callbacks :as callbacks]
            [twitter.callbacks.handlers :as handlers]
            [twitter.api.streaming :as streaming]
            [cheshire.core :as json]
            [clojure.tools.logging :refer [info error]]
            [amazonica.aws.kinesis :as kinesis]
            [clj-time.format :as format]
            [clj-time.coerce :refer [to-date]])
  (:import twitter.callbacks.protocols.AsyncStreamingCallback
           com.amazonaws.services.kinesis.model.ResourceInUseException)
  (:gen-class))

(def my-creds (oauth/make-oauth-creds "vbdFTliSzmnZowIhIn3Ww"
                                "u8jfFVmcGI7FRg7bisxIoIo1qCQHqnDIezAsMuuE"
                                "14907286-y6QYSNaOHaI3SeApcXDc8OrBUZ3W8FKaSutayVn9S"
                                "zup0zrZ8T1m3oMIRObI7UxfevLoJ5fYSQarqvxY0pGDsM"))

(def ^:dynamic *stream-name* "Twitter")

(def ^:dynamic *kinesis-uri* "https://kinesis.us-east-1.amazonaws.com")

;; make sure we have a working stream
(defn create-stream [stream-name]
  (let [streams (kinesis/list-streams)
        stream-names (set (:stream-names streams))]
    (if (stream-names stream-name)
      (do                               ;stream exists, check status
        (let [stream (kinesis/describe-stream stream-name)
              status (get-in stream [:stream-description :stream-status])]
          (case status
            ;; all is well
            "ACTIVE" :created
            ;; wait until active
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
        date (when date-str
               (to-date (format/parse (format/formatter "EEE MMM dd HH:mm:ss Z yyyy") date-str)))]
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

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (create-stream *stream-name*)
  (streaming/statuses-sample :oauth-creds my-creds
                             :callbacks async-streaming-callback
                             :params {:stall-warnings "true"}))
