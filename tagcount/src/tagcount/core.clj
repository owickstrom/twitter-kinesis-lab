(ns tagcount.core
  (:require [amazonica.aws.kinesis :refer [worker!]]
            [clojure.tools.logging :refer [debug info error]]
            [clj-time.core :refer [now minus minutes]]
            [clj-time.coerce :refer [to-date]]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(def ^:dynamic *region* "eu-west-1")

(def ^:dynamic *kinesis-uri* "https://kinesis.eu-west-1.amazonaws.com")

(def state (atom {}))

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

(def cred {:access-key (env :aws-access-key-id)
           :secret-key (env :aws-secret-key)
           :endpoint (env :aws-region)})

(def
  ^{:doc "upsert: update a record if it already exists, or insert a new record
 if it does not - all in a single statement; params are:
 count valid_to tag
 tag count valid_to"}
  upsert
  "WITH upsert AS
   (UPDATE tag_count
     SET count=?,
         valid_to=?
     WHERE tag=?
     RETURNING *)
   INSERT INTO tag_count (tag, count, valid_to)
    SELECT ?, ?, ?
    WHERE NOT EXISTS
    (SELECT * FROM upsert);")

(defn append-timestamp [state event]
  (let [timestamps (get state (:tag event) [])]
    (assoc state (:tag event) (conj timestamps (:created-at event)))))

(defn date-minutes-ago [n]
  (to-date (minus (now) (minutes n))))

(defn keep-newer [state limit]
  (into {} (map
            (fn [[tag timestamps]]
              [tag (filter (fn [t] (.before limit t)) timestamps)])
            state)))

(defn handle-event [state event]
  (let [old-timestamp (date-minutes-ago 75)]
    (-> state
        (append-timestamp event)
        (keep-newer old-timestamp))))

(defn process-records [records]
  (info "About to process" (count records) "records")
  (doseq [record records]
    (let [data (:data record)
          tag (:tag data)
          valid-to (java.sql.Timestamp. (.getTime (:created-at data)))
          _ (debug "Calculating sliding window:" data)
          new-state (swap! state handle-event data)
          count (-> new-state (get tag) count)]
      (info (.getName (Thread/currentThread)) "Saving to db:" tag "has count" count "at" valid-to)
      (try
        (jdbc/execute! pooled-db-spec
                       [upsert
                        count valid-to tag
                        tag count valid-to])
        (catch java.sql.SQLException e
          (error (.getNextException e))
          (throw e))))))

(defn start-workers [options]
  (dotimes [i (:workers options)]
    (let [uuid (worker! :credentials cred
                        :region-name *region*
                        :endpoint *kinesis-uri*
                        :app (:application options)
                        :stream (:stream options)
                        :processor process-records)]
      (info "Started worker" i "with uuid" uuid))))

;; command line stuff

(def cli-options
  [["-a" "--application APPNAME" "Kinesis application name; names DDB table (required)"]
   ["-s" "--stream STREAMNAME" "Kinesis stream name (required)"]
   ["-w" "--workers N" "Number of parallel Kinesis workers; <= # of Kinesis shards"
    :default 1
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 17) "Must be a number between 1 and 16, because ... I say so"]]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> [""
        "This is the tagcount application, that keeps a sliding window count of"
        "the Twitter hashtags retrieved from the given Kinesis stream."
        ""
        "You must specify a Kinesis application name that is unique for the AWS account and region,"
        "like TwitterTrends-<teamname>. You must also tell tagcount the name of the Kinesis stream"
        "where to find the hashtags. That would perhaps be Hashtags-<teamname>."
        ""
        "Usage: tagcount [-wh] -a APPNAME -s STREAMNAME"
        options-summary]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
     (:help options) (exit 0 (usage summary))
     (not
      (and
       (:stream options)
       (:application options))) (exit 1 (str \newline "Some options are required"
                                             \newline (usage summary)))
     errors (exit 1 (error-msg errors)))
    (start-workers options)))
