(ns tagcount.core
  (:require [amazonica.aws.kinesis :refer [worker!]]
            [clj-time.core :refer [now minus minutes]]
            [clj-time.coerce :refer [to-date]]))

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
    (worker! :app "jan-testar"
	            :stream "Twitter"
	            :processor (fn [records]
	                         (doseq [row records]
                            (println (:data row))
                            (swap! state handle-event (:data row)))))))

(defn -main
  [& argv]
    (do-something))
