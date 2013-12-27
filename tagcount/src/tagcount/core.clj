(ns tagcount.core
  (:require [amazonica.aws.kinesis :refer [worker!]]
            [clojure.tools.logging :refer [info error]]
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

(def
  ^{:doc "upsert: update a record if it already exists, or insert a new record
 if it does not - all in a single statement"}
  upsert
  "WITH upsert AS
   (UPDATE tag_count
     SET count=count+1,
         valid_to=?
     WHERE tag=?
     RETURNING *)
   INSERT INTO tag_count (tag, count, valid_to)
    SELECT ?, 1, ?
    WHERE NOT EXISTS
    (SELECT * FROM upsert);")

(defn process-records [records]
  (doseq [record records]
    (let [data (:data record)
          tag (:tag data)
          timestamp (java.sql.Timestamp. (.getTime (:created-at data)))]
      (info "Saving to db:" data)
      (try
        (jdbc/execute! pooled-db-spec
                       [upsert timestamp tag tag timestamp])
        (catch java.sql.SQLException e
          (error (.getNextException e))
          (throw e))))))

(defn start-worker []
  (let [state (atom {})]
    (worker! :app "TwitterAnalyzer"
             :stream "Twitter"
             :processor process-records)))

(defn -main
  [& argv]
    (start-worker))

;; example on using clojure.jdbc 0.3.0 api

(def test-upsert
  "WITH upsert AS
   (UPDATE spider_count SET tally=tally+1
    WHERE date='today' AND
          spider='Googlebot'
    RETURNING *)
   INSERT INTO spider_count (spider, tally)
    SELECT 'Googlebot', 1
     WHERE NOT EXISTS (SELECT * FROM upsert)")

(defn test-jdbc []
  (try
    (println)
    (println "dropping table")
    (jdbc/db-do-commands
     pooled-db-spec
     (jdbc/drop-table-ddl :spider_count))
    (println "creating table")
    (jdbc/db-do-commands
     pooled-db-spec
     (jdbc/create-table-ddl :spider_count
                            [:date :date "NOT NULL" "DEFAULT NOW()"]
                            [:spider :varchar "NOT NULL"]
                            [:tally :int "NOT NULL" "DEFAULT 0"]))
    (println "upserting"
             (jdbc/execute! pooled-db-spec [test-upsert])
             (jdbc/execute! pooled-db-spec [test-upsert])
             (jdbc/execute! pooled-db-spec [test-upsert]))
    (println (jdbc/query pooled-db-spec ["SELECT * FROM spider_count"]))
    (finally
      (println "dropping table")
      (jdbc/db-do-commands
       pooled-db-spec
       (jdbc/drop-table-ddl :spider_count)))))

(comment
  (test-jdbc)
  )
