(ns gantt-average.events
  (:require
   [re-frame.core :as rf]
   [gantt-average.db :as db]
   [gantt-average.utils :refer [date->ms]]))

;; random actors

(defn- random-timeline [ms-from ms-to average-slots-amount]
  (when (< ms-from ms-to)
    (let [rand-range (quot (- ms-to ms-from) average-slots-amount)]
      (loop [t ms-from
             r []]
        (let [t (+ t 1 (rand-int rand-range))]
          (if (> t ms-to)
            (->> r
                 (partition 2)
                 (mapv (fn [[s e]] {:start s
                                    :end e})))
            (recur t (conj r t))))))))

(defn- random-actors [ms-from ms-to actors-amount]
  (when (< ms-from ms-to)
    (->> (range actors-amount)
         (mapv (fn [i] {:name (str "actor " (inc i))
                        :timeline (random-timeline ms-from ms-to (+ 2 (rand-int 8)) ;; FIXME let it depends on days count?
                                                   )})))))

(defn- generate-random-actors [{:keys [date-from date-to]}]
  (when (and date-from date-to)
    (random-actors
     (date->ms date-from)
     (+ (date->ms date-to) (* 24 60 60 1000) -1)
     1000)))

(rf/reg-event-db
 :generate-random-actors
  (fn [db [_ params]]
    (let [params (merge (select-keys db [:date-from :date-to]) params)]
      (assoc db :actors (generate-random-actors params)))))

(rf/reg-event-db
 ::initialize-db
  (fn [_ _]
    (let [today-str (subs (.toISOString (js/Date.)) 0 10)
          db (assoc db/default-db
                    :date-from today-str
                    :date-to today-str
                    :grid-count 24)]
      (assoc db
             :actors (generate-random-actors db)))))

(rf/reg-event-db
 :set-values-by-paths
  (fn [db [_ paths-values]] (reduce (fn [a [k v]] ((if (vector? k) assoc-in assoc) a k v)) db paths-values)))
