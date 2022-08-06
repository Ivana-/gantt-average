(ns gantt-average.utils)

(defn date->ms [s] (when s (.parse js/Date s)))
(defn ms->date [n] (when (number? n) (js/Date. n)))

(defn ms->iso-str [n]
  (when-let [date (ms->date n)]
    (.toISOString date)))

(defn ms->str [n]
  (when-let [date (ms->date n)]
    (let [d (.getUTCDate date)
          h (.getUTCHours date)
          m (.getUTCMinutes date)
          ;; s (.getUTCSeconds date)
          frm (fn [x] (if (< x 10) (str "0" x) (str x)))]
      (str d " " (frm h) ":" (frm m)))))
