(ns galileo.dash
  (:require [cljs.nodejs :as nodejs]
            [galileo.comm :as comm :refer [pass->]]
            [cljs.core.async :as a :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defonce blessed
         (nodejs/require "blessed"))

(defonce contrib
         (nodejs/require "blessed-contrib"))

(defonce screen
         (.screen blessed))

(defonce grid
         (new contrib.grid #js {:rows 2 :cols 2}))

(defonce grid-left
         (new contrib.grid #js {:rows 2 :cols 1}))

(defn- create-layout []

  (.set grid-left 0 0 1 1
        contrib.table
        #js {:label         "Connected Beans"
             :fg            "green"
             :columnSpacing 16
             :keys          true})

  (.set grid-left 1 0 1 1
        contrib.log
        #js {:label      "Event Log"
             :fg         "green"
             :selectedFg "green"})

  (.set grid 0 1 1 1
        contrib.sparkline
        #js {:label "Sensor History"
             :tags  true
             :style #js {:fg "blue"}})

  (.set grid 1 1 1 1
        contrib.log
        #js {:label      "OSC Log"
             :fg         "green"
             :selectedFg "green"})

  (.set grid 0 0 1 2 grid-left)

  (.applyLayout grid screen))

(defn prog-log
  "Prints data from the program into the dashboard."
  [& args]
  (let [event-log (.get grid-left 1 0)
        entry (apply str args)]
    (.log event-log entry)))

(defn osc-log
  "Prints what is sent via OSC into the dashboard."
  [& args]
  (let [event-log (.get grid 1 1)
        entry (apply str args)]
    (.log event-log entry)))

(defn start-osc-log
  "Listens for new messages on comm/osc-chan."
  []
  (go-loop []
           (when-let [v (<! comm/osc-chan)]
             (osc-log (:msg v))                             ;; :msg contains the above :response
             (recur))))

(defn start-prog-log
  "Listens for new messages on comm/log-chan."
  []
  (go-loop []
           (when-let [v (<! comm/log-chan)]
             (prog-log (:msg v))                            ;; :msg contains the above :response
             (recur))))

(defn build-history [[_ {:keys [localName sensors]}]]
  (let [{x :x y :y z :z} sensors]
    (into [] x)))

(defn build-table [[_ {:keys [localName sensors]}]]
  (let [{x :x y :y z :z} sensors]
    (conj [] localName (last x) (last y) (last z))))

(defn update-graphs [peripherals]
  (let [sensor-history (.get grid 0 1)
        hist-header #js ["bean-a" "bean-b" "bean-c"]
        device-table   (.get grid-left 0 0)]
    (.setData device-table
              (clj->js {:headers ["name" "x" "y" "z"]
                        :data    (do
                                   (pass-> :log (first (mapv build-table peripherals)))
                                   (conj [] (mapv build-table peripherals)))}))
    #_(.setData sensor-history hist-header
              (clj->js [(mapv build-history peripherals)]))))

(defn- init-data []
  (let [sensor-history (.get grid 0 1)
        device-table (.get grid-left 0 0)
        hist-header #js ["bean-a" "bean-b" "bean-c"]
        hist-data (clj->js
                    [[5 6 7 0]
                     [5 6 7 0]
                     [5 6 7 0]])
        table-data (clj->js
                     {:headers ["name" "x" "y" "z"]
                      :data    [["no-beans" 0 0 0]]})]
    (.focus device-table)
    (.setData device-table table-data)
    (.setData sensor-history hist-header hist-data)))

(defn start []
  (create-layout)
  (start-prog-log)
  (start-osc-log)
  (init-data)
  (.render screen))