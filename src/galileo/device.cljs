(ns galileo.device
  "### Primary Namespace

  Uses the node/iojs [Noble](https://github.com/sandeepmistry/nobl) library to interface with BLE peripherals."
  (:require [cljs.nodejs :as nodejs]
            [galileo.comm :as comm :refer [pass->]]
            [galileo.osc :as osc]
            [galileo.dash :as dash]
            [galileo.sample :as sample]
            [cljs.core.async :as a :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defonce noble
         (nodejs/require "noble"))

(declare exit-handler chain)

(defonce bean-uuid "a495ff10c5b14b44b5121370f02d74de")
(defonce js-bean-uuid #js [bean-uuid])

(defonce scratch-one "a495ff21c5b14b44b5121370f02d74de") ;; The three BLE scratch characteristics for Light Blue Beans
(defonce scratch-two "a495ff22c5b14b44b5121370f02d74de")
(defonce scratch-thr "a495ff23c5b14b44b5121370f02d74de")

(def peripherals (atom {}))
(def exiting (atom false)) ;; TO-DO consider a non-global-state solution.

(defn start-chain
  "Listens for new steps on comm/device-chan." []
  (go-loop []
           (when-let [v (<! comm/device-chan)]
             (chain (first (:msg v)))                       ;; :msg contains the above :response
             (recur))))

(defmulti chain
          "
This represents a chain of events that establishes and reads scratch
characteristic data from the BLE devices.

### The Chain:

1. `:discover` - Locates a BLE device.
2. `:connect` - Connects to a valid device. By UUID.
3. `:discover-service` - Discover device properties.
4. `:setup-service` - Discover server characteristics.
5. `:discovered-characteristics` - validate BLE device scratch data.
6. `:request-notify` - Requests scratch data from device.
7. `:process-device-data` - Sends BLE scratch data to OSC output.

          "
          #(:step %))

(defmethod chain :discover
  [{:keys [peripheral]}]
  (let [advertisement (js->clj (.-advertisement peripheral) :keywordize-keys true)
        uuid (.-uuid peripheral)]
    (pass-> :log "Scanning" uuid)
    (when (= "quipu" (:localName advertisement))
      (pass-> :log "Scanned" uuid)
      (.connect peripheral (fn [e]))
      (if-not ((keyword uuid) @peripherals)
        (.on peripheral
             "connect"
             (fn [e]
               (pass-> :device
                       {:step          :connect
                        :device        peripheral
                        :uuid          uuid
                        :advertisement advertisement})))
        (.stopScanning noble)))))

(defmethod chain :connect
  [{:keys [device uuid advertisement]}]
  (let [peripheral {(keyword uuid)
                    (merge advertisement
                           {:bean device})}]
    (swap! peripherals merge @peripherals peripheral)
    (.on device                                             ;; Current reconnect check, TO-DO: Enable connection to more than just the previously connected peripherals
         "disconnect"
         (fn [err] (when (false? @exiting)
                     (pass-> :log uuid "disconnected")
                     (.startScanning noble js-bean-uuid false))))
    (js/setTimeout
      (fn []                                                ;; TO-DO: Consider a better check for connection than time.
        (pass-> :device
                {:step       :discover-service
                 :device     device
                 :device-key (keyword uuid)})
        5000))))

(defmethod chain :discover-service
  [{:keys [device device-key]}]
  (pass-> :log "Descovering service for" device-key)
  (.discoverServices device #js []
                     (fn [err services]
                       (pass-> :log "Discovered service for" device-key)
                       (pass-> :device
                               {:step       :setup-service
                                :err        err
                                :services   services
                                :device-key device-key}))))

(defmethod chain :setup-service
  [{:keys [err services device-key]}]
  (if err
    (js/throw err))
  (.forEach services
            (fn [service]
              (if (= service.uuid bean-uuid)
                (pass-> :log "Discovering characteristics for" device-key)
                (.discoverCharacteristics
                  service
                  #js [scratch-one scratch-two scratch-thr]
                  (fn [err characteristics]
                    (pass-> :device
                            {:step            :discovered-characteristics
                             :err             err
                             :characteristics characteristics
                             :device-key      device-key})))))))

(defmethod chain :discovered-characteristics
  [{:keys [err characteristics device-key]}]
  (if err
    (js/throw err))
  (when-not (empty? (js->clj characteristics))
    (pass-> :log "Requesting characteristics for" device-key)
    (pass-> :device
            {:step           :request-notify
             :characteristic (aget characteristics 0)
             :axis           :x
             :device-key     device-key})
    (pass-> :device
            {:step           :request-notify
             :characteristic (aget characteristics 1)
             :axis           :y
             :device-key     device-key})
    (pass-> :device
            {:step           :request-notify
             :characteristic (aget characteristics 2)
             :axis           :z
             :device-key     device-key})))

(defmethod chain :request-notify
  [{:keys [characteristic axis device-key]}]
  (.on characteristic
       "read"
       (fn [raw _]
         (let [data (+ (bit-shift-left (aget raw 1) 8) (aget raw 0))]
           (pass-> :device
                   {:step       :pass-device-data
                    :data       data
                    :axis       axis
                    :device-key device-key}))))
  (.notify characteristic
           true
           (fn [error]
             (if error
               (pass-> :log "There was an Error in Scratch Data")
               nil))))

(defn sensor-history [prev curr]
  (if (vector? prev)
    (if (>= (count prev) 60)
      (conj (subvec prev 1) curr)
      (conj prev curr))
    [curr]))

(defmethod chain :pass-device-data
  [{:keys [data axis device-key]}]
  (swap! peripherals #(update-in % [device-key :sensors axis] (fn [prev] (sensor-history prev data))))
  (osc/send-data device-key axis data))

(defn init-with-devices
  "When not running on sample data begins scanning for BLE Devices using node/noble"
  [scan-timeout]
  (pass-> :log "Starting with Devices")

  (.startScanning noble js-bean-uuid false)

  (js/setTimeout (fn []                                     ; Stops scanning after a set time. TO-DO: replace
                   (pass-> :log "Stopped Scanning")
                   (.stopScanning noble))
                 scan-timeout)

  (.on noble "discover" #(pass-> :device {:step       :discover ; Once any device is discovered, pass it to channel.
                                          :peripheral %})))

(defn start-dashboard-updates
  "In order to overcome the near-random flood of data from the BLE devices,
   this updates the dashboard graphs at a set interval."
  [interval]
  (js/setInterval (fn []
                    (dash/update-graphs @peripherals))
                  interval))

(defn main
  "As of CLJS 2850 this is the main entrypoint"
  [& args]

  (comm/begin-subscriptions)
  (start-chain)
  (dash/start)
  (.key dash/screen #js ["escape", "q", "C-c"] exit-handler)
  (osc/init-osc)

  "If this is a sample run, don't start a Noble BLE scan."
  (if (some #(= "sample" %) args)
    (sample/start [:device1 :device2 :device3] 100 100)
    (init-with-devices 12000))

  (start-dashboard-updates 100))

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

(set! *main-cli-fn* main)