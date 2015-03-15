(ns galileo.sample
  (:require [galileo.comm :as comm :refer [pass->]]))

(defn sample-run
  "Allows for device-less testing."
  [device-set]
  (let [sample-data [0 10 99 33.3 200 100 30 24 1 2 88 54 34 21 150 130 120 190]
        sample-axis [:x :y :z]]
    (js/setInterval
      (fn []
        (dorun
          (map
            (fn [[data axis device-key]]
              (pass-> :device {:step        :pass-device-data
                               :data        data
                               :axis        axis
                               :device-key  device-key}))
            (map
              (fn [device-key]
                [(rand-nth sample-data)
                 (rand-nth sample-axis)
                 device-key])
              device-set))))
      100)))

(defn start [device-set]
  (js/setTimeout (fn []
                   (pass-> :log "Starting Sample Run")
                   (sample-run device-set))
                 100))