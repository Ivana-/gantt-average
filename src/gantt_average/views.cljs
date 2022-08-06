(ns gantt-average.views
  (:require [re-frame.core :as rf]
            [gantt-average.widgets :as ws]
            [gantt-average.utils :refer [date->ms ms->date ms->str]]
            [clojure.string :as str]
            [cljs.reader :as reader]
            [reagent.core :as r]
            ["chart.js"]))

(defn- add-actors-timeline [data grid timeline actors-count]
  (loop [[grid-l grid-r :as grid] grid
         [{:keys [start end]} :as timeline] timeline
         result data]
    (cond
      (nil? grid-r) result
      (empty? timeline) result
      (< end grid-l) (recur grid (rest timeline) result)
      (> start grid-r) (recur (rest grid) timeline result)

      ;; overlap case
      :else (let [updated-result (update result grid-l (fnil + 0)
                                         (/ (- (min grid-r end) (max grid-l start))
                                            (- grid-r grid-l)
                                            actors-count))]
              (if (< end grid-r)
                (recur grid (rest timeline) updated-result)
                (recur (rest grid) timeline updated-result))))))

(defn- gantt-average [{:keys [date-from date-to grid-count actors-count actors]}]
  (when (and date-from
             date-to
             (number? grid-count)
             (number? actors-count)
             (coll? actors))
    (let [start-ms (js/Date.)
          ms-from (date->ms date-from)
          ms-to (+ (date->ms date-to) (* 24 60 60 1000))
          grid-step (quot (- ms-to ms-from) grid-count)
          grid (mapv #(+ ms-from (* % grid-step)) (range (inc grid-count)))
          data (zipmap (butlast grid) (repeat 0))
          result (->> actors
                      (take actors-count)
                      (reduce (fn [acc {:keys [timeline]}] (add-actors-timeline acc grid timeline actors-count)) data)
                      (sort-by first)
                      vec)]
      (prn (- (js/Date.) start-ms) :ms)
      result)))

(defn- chartjs-component [params]
  (r/with-let [id (str (or (:id params) (str "chart_" (gensym))))
               state (r/atom nil)]
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [context (.getContext (.getElementById js/document id) "2d")
                                   chart (js/Chart. context (clj->js (r/props this)))]
                               (swap! state assoc :context context :chart chart)))
      :component-did-update (fn [this _]
                              (when-let [{:keys [context chart]} @state]
                                (when chart (.destroy chart))
                                (swap! state assoc :chart (js/Chart. context (clj->js (r/props this))))))
      :component-will-unmount (fn [_this]
                                (when-let [chart (:chart @state)] (.destroy chart)))
      :display-name "chartjs-component"
      :reagent-render (fn [] [:div {:style {:position :relative
                                            :width "100%"
                                            :height "100%"}}
                              [:canvas {:id id}]])})))

(defn- timeline->edit-string [timeline]
  (str "[\n"
       (->> timeline
            (map (fn [{:keys [start end]}] (str (mapv ms->date [start end]))))
            (str/join "\n"))
       "\n]"))

(defn- edit-string->timeline [s]
  (try
    (let [data (->> (reader/read-string s)
                    (mapv (fn [[x y z]] (when (and (inst? x) (inst? y) (nil? z))
                                          {:start (long x)
                                           :end (long y)}))))]
      (when (every? map? data) data))
    (catch js/Error _
      nil)))

(defn main-panel []
  [:div {:style {:display :flex
                 :flex-direction :column
                 :gap "10px"
                 :overflow-y :hidden
                 :position :absolute
                 :top 0
                 :bottom 0
                 :left 0
                 :right 0
                 :padding "10px"}}
   [:h1 {:style {:margin 0}} "Gantt average task"]

   ;; params
   [:div {:style {:display :flex
                  :gap "20px"}}
    [:div
     [:div "from"] (ws/date {:class :input :path [:date-from]
                             :on-change #(rf/dispatch [:generate-random-actors {:date-from %}])})]
    [:div
     [:div "to"] (ws/date {:class :input :path [:date-to]
                           :on-change #(rf/dispatch [:generate-random-actors {:date-to %}])})]
    [:div
     [:div "grid count"] (ws/positive-int {:class :input :path [:grid-count]})]]

   ;; textarea & chart
   [:div {:style {:display :flex
                  :gap "20px"}}
    (let [{:keys [selected-actor-index timeline-edit-error]}
          @(rf/subscribe [:get-values-by-paths {:selected-actor-index :selected-actor-index
                                                :timeline-edit-error :timeline-edit-error}])]
      [:div {:style {:flex 1
                     :display :flex
                     :flex-direction :column}}
       [:div (if selected-actor-index
               (str "Actor #" (inc selected-actor-index))
               "Select actor to edit his timeline data")]
       (ws/textarea {:style (merge {:flex 1}
                                   (when timeline-edit-error
                                     {:color :red}))
                     :disabled (nil? selected-actor-index)
                     :path [:timeline-edit]
                     :on-change (fn [s]
                                  (when selected-actor-index
                                    (if-let [v (edit-string->timeline s)]
                                      (rf/dispatch [:set-values-by-paths {[:actors selected-actor-index :timeline] v
                                                                          :timeline-edit-error nil}])
                                      (rf/dispatch [:set-values-by-paths {:timeline-edit-error true}]))))})])

    (let [{:keys [date-from date-to grid-count actors-count actors selected-actor-index]}
          @(rf/subscribe [:get-values-by-paths {:date-from :date-from
                                                :date-to :date-to
                                                :grid-count :grid-count
                                                :actors-count :actors-count
                                                :actors :actors
                                                :selected-actor-index :selected-actor-index}])
          average-data (gantt-average (merge {:date-from date-from
                                    :date-to date-to
                                    :grid-count grid-count}
                                   (if selected-actor-index
                                     {:actors-count 1
                                      :actors [(get actors selected-actor-index)]}
                                     {:actors-count actors-count
                                      :actors actors})))]
      [:div {:style {:flex 1}}
       [chartjs-component {:type "bar"
                           :data {:labels (->> average-data keys (mapv ms->str))
                                  :datasets [{:data (->> average-data vals)
                                              :label (if selected-actor-index
                                                       (str "Actor #" (inc selected-actor-index))
                                                       (str "First " actors-count " actors"))
                                              :backgroundColor "#90EE90"}]}
                           :options {:scales {:x {:grid {:display false}}
                                              :y {:min 0,
                                                  :max 1}}}}]])]

   ;; actors
   (let [{:keys [actors selected-actor-index]}
         @(rf/subscribe [:get-values-by-paths {:actors :actors
                                               :selected-actor-index :selected-actor-index}])]
     [:<>
      [:div {:style {:display :flex
                     :align-items :baseline}}
       [:input {:type "radio"
                :id "actors_all"
                :name "actors"
                :class :mrxs
                :checked (nil? selected-actor-index)
                :on-change (fn [_]
                             (rf/dispatch [:set-values-by-paths {:selected-actor-index nil
                                                                 :timeline-edit nil
                                                                 :timeline-edit-error nil}]))}]
       [:label.mlm.bold {:for "actors_all"
                         :style {:flex 1
                                 :display :flex}}
        [:div {:style {:font-weight :bold :margin "0 10px 0 5px"}}
         "First"]
        (ws/positive-int {:class :input :path [:actors-count]})
        [:div {:style {:font-weight :bold :margin-left "10px"}}
         "actors"]]]

      [:div {:style {:flex 1
                     :position :relative}}
       [:div {:style {:position :absolute
                      :top 0
                      :bottom 0
                      :overflow-y :auto}}
        (->> actors
             (map-indexed (fn [i {:keys [timeline]}]
                            (let [input-id (str "actor" i)]
                              [:div {:key i
                                     :style {:display :flex
                                             :align-items :baseline
                                             :margin-bottom "5px"}}
                               [:input {:type "radio"
                                        :id input-id
                                        :name "actors"
                                        :class :mrxs
                                        :checked (= i selected-actor-index)
                                        :on-change (fn [_]
                                                     (rf/dispatch [:set-values-by-paths
                                                                   {:selected-actor-index i
                                                                    :timeline-edit (timeline->edit-string timeline)
                                                                    :timeline-edit-error nil}]))}]
                               [:label {:for input-id
                                        :style {:flex 1
                                                :display :flex}}
                                [:div {:style {:font-weight :bold :margin "0 10px 0 5px"}}
                                 (-> i inc str)]
                                [:div {:style {:flex 1}} (str timeline)]]])))
             doall)]]])])
