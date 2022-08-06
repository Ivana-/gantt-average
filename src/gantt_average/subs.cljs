(ns gantt-average.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :get-values-by-paths
  (fn [db [_ keys-paths]] (reduce (fn [a [k p]] (assoc a k ((if (vector? p) get-in get) db p))) {} keys-paths)))
