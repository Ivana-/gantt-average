(ns gantt-average.widgets
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

(rf/reg-sub
 ::get-value
 (fn [db [_ path]] (get-in db path)))

(rf/reg-event-db
 ::set-value
 (fn [db [_ path v]] (assoc-in db path v)))

(defn input-wrapper [{:keys [tag f-value] :as m}]
  (fn [{:keys [path on-change] :as params}]
    [(or tag :input)
     (-> params
         (dissoc :path)
         (merge (dissoc m :tag :f-value))
         (assoc
          :value (str @(rf/subscribe [::get-value path]))
          :on-change #(let [v (.. % -target -value)]
                        (rf/dispatch [::set-value path (if f-value (f-value v) v)])
                        (when on-change (on-change v)))))]))

(def text (input-wrapper {:type "text"}))
(def textarea (input-wrapper {:tag :textarea}))

(defn select [{:keys [path on-change items] :as params}]
  (let [value (str @(rf/subscribe [::get-value path]))]
    (into
     [:select (-> params
                  (dissoc :path :items)
                  (assoc
                   :value value
                   :on-change #(let [v (.. % -target -value)]
                                 (rf/dispatch [::set-value path v])
                                 (when on-change (on-change v)))))
      [:option ""]
      (when (and (not (str/blank? value)) (not (contains? (set items) value))) [:option value])]
     (mapv (fn [x] [:option x]) items))))

(defn checkbox [{:keys [path on-change] :as params}]
  [:input (-> params
              (dissoc :path)
              (assoc
               :type "checkbox"
               :checked (= "true" (str @(rf/subscribe [::get-value path])))
               :on-change #(let [v (.. % -target -checked)]
                             (rf/dispatch [::set-value path v])
                             (when on-change (on-change v)))))])

(def date (input-wrapper {:type "date"}))
(def time-time (input-wrapper {:type "time"}))

(defn date-time [{:keys [path on-change] :as params}]
  (let [v (str @(rf/subscribe [::get-value path]))
        cnt-v (count v)
        value (cond
                (= 0 cnt-v) ""
                (>= cnt-v 16) (subs v 0 16)
                :else (str v (subs "2018-01-01T00:00" cnt-v)))]
    [:input (-> params
                (dissoc :path)
                (assoc
                 :type "datetime-local"
                 :value value
                 :on-change #(let [v (str (.. % -target -value) ":00Z")]
                               (rf/dispatch [::set-value path v])
                               (when on-change (on-change v)))))]))

;; js/isNaN
(def num-conf {:type "number" :f-value #(js/parseFloat %)})
(def decimal (input-wrapper (assoc num-conf :step "0.000000000000001")))
(def integer (input-wrapper num-conf))
(def unsigned-int (input-wrapper (assoc num-conf :min "0")))
(def positive-int (input-wrapper (assoc num-conf :min "1")))