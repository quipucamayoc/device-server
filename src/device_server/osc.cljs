(ns device-server.osc
  (:require [cljs.nodejs :as nodejs]
            [device-server.comm :as comm :refer [pass->]]))

(defonce osc
         (nodejs/require "osc-min"))

(defonce udp
         (nodejs/require "dgram"))

(defonce config
         {:udpSock (.createSocket udp "udp4")
          :inport  9800
          :outport 9801})

(def ip (atom "127.0.0.1"))
(def once (atom true))

(defn establish-server [msg]
  (if (and (= (aget msg "address") "/my-addr") @once)
    (do (reset! ip (aget msg "args" 0 "value"))
        (pass-> :log "IP Set to: " (aget msg "args" 0 "value"))
        (pass-> :log msg)
        (reset! once false))
    nil))

(defn send-data [bean-key number data]
  (let [wrt (clj->js {:address "/beans" :args [bean-key number data]})
        buf (.toBuffer osc wrt)
        sock (:udpSock config)]
    (.send sock buf 0 buf.length (:outport config) @ip)))

(defn init-osc []
  (let [sock (.createSocket
               udp "udp4" (fn [msg rinfo]
                            (try
                              (establish-server (.fromBuffer osc msg))
                              (catch js/Error e
                                (pass-> :log "Invlaid Packet in UDP")
                                (pass-> :log e)))))]
    (.bind sock (:inport config))))

