(ns tagcount.migration
  (:require [clojure.java.jdbc :as jdbc]
            [tagcount.core :as core]))

(defn table-exists? [name]
  (jdbc/query core/pooled-db-spec
              ["select count(*) 
               from information_schema.tables 
               where table_schema = 'public'
               and table_name = ?" name]
              :row-fn #(> (:count %) 0)))

(defn create-tagcount-table []
  (if (not (table-exists? "tag_count"))
    (do
      (println "Creating table tag_count...")
      (jdbc/db-do-commands core/pooled-db-spec
                           (jdbc/create-table-ddl
                             :tag_count
                             [:tag "varchar(50)" "PRIMARY KEY"]
                             [:count "int" "NOT NULL"]
                             [:valid_to "timestamptz" "NOT NULL"])))
    (println "Table tag_count already exists.")))

(defn -main []
  (create-tagcount-table)
  (println "Done!"))
