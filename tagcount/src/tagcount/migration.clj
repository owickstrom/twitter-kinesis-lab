(ns tagcount.migration
  (:require [clojure.java.jdbc :as jdbc]
            [tagcount.core :as core]))

(defn table-exists? [name]
  (first
   (jdbc/query core/pooled-db-spec
               ["select count(*)
                from information_schema.tables
                where table_schema = 'public'
                and table_name = ?" name]
               :row-fn #(> (:count %) 0))))

(defn create-table [table]
  (if-not (table-exists? table)
    (do
      (println "Creating table" table)
      (jdbc/db-do-commands core/pooled-db-spec
                           (jdbc/create-table-ddl
                             (keyword table)
                             [:tag "varchar(140)" "PRIMARY KEY"]
                             [:count "int" "NOT NULL" "DEFAULT 0"]
                             [:valid_to "timestamptz" "NOT NULL"])))
    (println "Table" table "already exists.")))

(defn -main []
  (create-table "tag_count")
  (println "Done!"))
