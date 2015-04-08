(ns galileo.comm
  (:require [cljs.core.async
             :as a
             :refer [sub chan pub <! >! sliding-buffer]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defonce content-stream (chan (sliding-buffer 1024)))

(defn pass-> [topic & args]
  (go (>! content-stream {:topic topic
                          :msg   args})))

(defonce watch-stream-for
         (pub content-stream #(:topic %)))

(defonce osc-chan (chan (sliding-buffer 1024)))
(defonce log-chan (chan (sliding-buffer 1024)))
(defonce device-chan (chan (sliding-buffer 1024)))

(defn begin-subscriptions []
  (sub watch-stream-for :osc osc-chan)
  (sub watch-stream-for :log log-chan)
  (sub watch-stream-for :device device-chan))


(defn start-osc-log
  "Listens for new messages on comm/osc-chan."
  []
  (go-loop []
           (when-let [v (<! osc-chan)]
             (comment .log js/console (:msg v))
             (recur))))

(defn start-prog-log
  "Listens for new messages on comm/log-chan."
  []
  (go-loop []
           (when-let [v (<! log-chan)]
             (.log js/console (:msg v))
             (recur))))