(ns galileo.device
  (:require [cljs.nodejs :as nodejs]
            [galileo.comm :as comm :refer [pass->]]
            [galileo.osc :as osc]
            [galileo.dash :as dash]
            [cljs.core.async :as a :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defonce noble
         (nodejs/require "noble"))

(declare exit-handler)

(defonce bean-uuid "a495ff10c5b14b44b5121370f02d74de")
(defonce js-bean-uuid #js [bean-uuid])

(defonce scratch-one "a495ff21c5b14b44b5121370f02d74de")
(defonce scratch-two "a495ff22c5b14b44b5121370f02d74de")
(defonce scratch-thr "a495ff23c5b14b44b5121370f02d74de")

(def peripherals (atom {}))
(def exiting (atom false))

(defn process-bean-data
  "Sends data to osc/"
  [raw axis bean-key]
  (let [data (+ (bit-shift-left (aget raw 1) 8) (aget raw 0))]
    (osc/send-data bean-key axis data)))

(defn request-notify
  "Noble.
   Callback when bean responds."
  [characteristic axis bean-key]
  (.on characteristic "read" (fn [data _]
                               (process-bean-data data axis bean-key)))
  (.notify characteristic "true" (fn [error]
                                   (if error
                                     (pass-> :log "There was an Error in Scratch Data")
                                     nil))))

(defn setup-service
  "Noble.
   Finally begins characteristics data gathering."
  [err services bean-key]
  (if err
    (js/throw err))
  (.forEach services (fn [service]
                       (if (= service.uuid bean-uuid)
                         (pass-> :log "Discovering characteristics for" bean-key)
                         (.discoverCharacteristics
                             service
                             #js [scratch-one scratch-two scratch-thr]
                             (fn [error characteristics]
                               (if error
                                 (js/throw error))
                               (when-not (empty? (js->clj characteristics))
                                 (pass-> :log "Requesting characteristics for" bean-key)
                                 (request-notify (aget characteristics 0) :x bean-key)
                                 (request-notify (aget characteristics 1) :y bean-key)
                                 (request-notify (aget characteristics 2) :z bean-key))))))))

(defn discover-service
  "Noble.
   Callback fired when characteristics are discovered."
  [item bean-key]
  (pass-> :log "Descovering service for" bean-key)
  (.discoverServices item #js [] (fn [err services]
                                    (pass-> :log "Discovered service for" bean-key)
                                    (setup-service err services bean-key))))

(defn connect
  "Noble.
   Callback fired when the connection is finalized.
   Characteristics can be discovered."
  [bean uuid advertisement]
  (let [peripheral {(keyword uuid)
                    (merge advertisement
                           {:bean bean})}]
    (swap! peripherals merge @peripherals peripheral)
    ;; Current reconnect check, TO-DO: Enable connection to more than just the previously connected peripherals
    (.on bean "disconnect" (fn [err] (when (false? @exiting)
                                       (pass-> :log uuid "disconnected")
                                       (.startScanning noble js-bean-uuid false))))
    (js/setTimeout (fn []
                     (discover-service bean (keyword uuid))
                   12000))))

(defn discover
  "Noble.
   A callback fired when a peripheral is discovered."
  [peripheral]
  (let [advertisement (js->clj (.-advertisement peripheral) :keywordize-keys true)
        uuid (.-uuid peripheral)]
    (pass-> :log "Scanning" uuid)
    (when (= "quipu" (:localName advertisement))
      (pass-> :log "Scanned" uuid)
      (.connect peripheral (fn [e]))
      (if-not ((keyword uuid) @peripherals)
        (.on peripheral "connect" (fn [e]
                                    (connect peripheral uuid advertisement)))
        (.stopScanning noble)))))

(defn -main
  "As of CLJS 2850 this is the main entrypoint"
  []
  ; Start Internal Communications
  (comm/begin-subscriptions)
  ; Starts the dashboard visualization.
  (dash/start)
  ; Provides a separate termination handler, to disconnect from the devices.
  (.key dash/screen #js ["escape", "q", "C-c"] exit-handler)
  ; Begins OSC Communication
  (osc/init-osc)
  ; Begins the scan process for surrounding BLE devices. Based on the UUID.
  (.startScanning noble js-bean-uuid false)
  ; Stops scanning after a set time. TO-DO: replace
  (js/setTimeout (fn []
                   (pass-> :log "Stopped Scanning")
                   (.stopScanning noble))
                 5000)
  ; Once any device is discovered, fire discover.
  (.on noble "discover" discover))

(defn exit-handler
  "Cleanly disconnects from the beans before terminating the node process."
  []
  (pass-> :log "Exiting")
  (reset! exiting true)
  (.stopScanning noble)

  (doseq [[uuid {bean :bean}] @peripherals]
    (do (pass-> :log "Disconnecting from " uuid)
        (.disconnect bean #(pass-> :log "disconnected"))))

  (js/setTimeout (fn []
                   (.exit js/process))
                 2000))

(set! *main-cli-fn* -main)