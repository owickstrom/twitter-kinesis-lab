(ns tagcount.core
  (:require [amazonica.aws.kinesis :refer [worker!]]
            [clj-time.core :refer [now minus minutes]]
            [clj-time.coerce :refer [to-date]]
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

(defn append-timestamp [state event]
  (let [timestamps (get state (:tag event) [])]
    (assoc state (:tag event) (conj timestamps (:created-at event)))))

(defn display-top [state k]
  (println (take k (sort-by (fn [[key c]] (- c)) (map (fn [[key v]] [key (count v)]) state))))
  state)

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
      (keep-newer old-timestamp)
      (display-top 10))))

(defn do-something []
  (let [state (atom {})]
    (worker! :app "TwitterAnalyzer"
             :stream "Twitter"
             :processor (fn [records]
                          (doseq [row records]
                            (println (:data row))
                            (swap! state handle-event (:data row)))))))

(defn -main
  [& argv]
    (do-something))

;; example on using clojure.jdbc 0.3.0 api
(defn test-jdbc []
  (try
    (println)
    (println "creating table")
    (jdbc/db-do-commands
     pooled-db-spec
     (jdbc/create-table-ddl :berries
                            [:name "varchar(32)" "PRIMARY KEY"]
                            [:price :int]))
    (println "inserting berries"
             (jdbc/insert! pooled-db-spec :berries {:name "strawberries" :price 150})
             (jdbc/insert! pooled-db-spec :berries {:name "blackberries" :price 170})
             (jdbc/insert! pooled-db-spec :berries {:name "raspberries" :price 120}))
    (println (jdbc/query pooled-db-spec ["SELECT * FROM berries WHERE price > ? ORDER BY price" 120]))
    (println "updating price"
             (jdbc/update! pooled-db-spec :berries {:price 122} ["name = ?" "raspberries"]))
    (println (jdbc/query pooled-db-spec ["SELECT * FROM berries WHERE price > ? ORDER BY price" 120]))
    (println "deleting strawberries"
             (jdbc/delete! pooled-db-spec :berries ["name = ?" "strawberries"]))
    (println (jdbc/query pooled-db-spec ["SELECT * FROM berries WHERE price > ? ORDER BY price" 120]))
    (finally
      (println "dropping table")
      (jdbc/db-do-commands
       pooled-db-spec
       (jdbc/drop-table-ddl :berries)))))

(comment
  (test-jdbc)
  )
