(ns galileo.dash
  (:require [cljs.nodejs :as nodejs]
            [galileo.comm :as comm :refer [pass->]]
            [cljs.core.async :as a :refer [<!]]
            [goog.object :as o])
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
        #js {:style #js {:text "green"}
             :label      "OSC Log"
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

(defn clean-axis [axis]
    (if (nil? axis)
      [0]
      axis))

(defn device-history-data
  "Takes a device map and returns axis data. Todo, return all sensors agnostically."
  [[_ {:keys [sensors]}]]
  (let [{x :x y :y z :z} sensors]
    [(clean-axis x) (clean-axis y) (clean-axis z)]))

(defn device-history-names
  "Merges device UUIDs and sensor names."
  [peripherals]
  (apply concat
         (map (fn [device-key]
                (map #(str device-key "-" %)
                     (keys (:sensors (device-key peripherals)))))
              (keys peripherals))))

(defn clean-last-axis [axis]
  (let [last-axis (last axis)]
    (if (nil? last-axis)
      0
      last-axis)))

(defn build-row
  "Returns a row containing only latest sensor values from device. TO-DO: Remove hardcoded sensors"
  [[_ {:keys [localName sensors]}]]
  (let [{x :x y :y z :z} sensors]
    [(str localName) (clean-last-axis x) (clean-last-axis y) (clean-last-axis z)]))

(defn update-graphs
  "Updates the gaphics that represent real-time & over-time device status and sensor values."
  [peripherals]
  (let [sensor-history (.get grid 0 1)
        hist-header (device-history-names peripherals)
        hist-data (mapv vec (apply concat (map device-history-data peripherals)))
        device-table (.get grid-left 0 0)
        device-table-data (mapv build-row peripherals)]

    ; Update list of devices and sensor values.
    (.setData device-table
              (clj->js {:headers ["name" "x" "y" "z"]
                        :data    device-table-data}))
    ; Update sparkline of sensor values.
    (.setData sensor-history
              (clj->js hist-header)
              (clj->js hist-data))
    ; Needed to update the screen when graphics change.
    (.render screen)))

(defn- init-data
  "Starts the Dashboard." []
  (let [sensor-history (.get grid 0 1)
        device-table (.get grid-left 0 0)
        hist-header (clj->js [""])
        hist-data (clj->js
                    [[]])
        table-data (clj->js
                     {:headers ["name" "x" "y" "z"]
                      :data    [["no-devices" 0 0 0]]})]
    (.focus device-table)
    (.setData device-table table-data)
    (.setData sensor-history hist-header hist-data)))

(defn start []
  (create-layout)
  (start-prog-log)
  (start-osc-log)
  (init-data)
  (.render screen))