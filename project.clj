(defproject com.quipucamayoc/device-server "1.0.5"
            :description "The central communication node between wearable devices and
  the rest of the project."
            :url "http://quipucamayoc.com/"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}

            :dependencies [[org.clojure/clojure "1.8.0-alpha4"]
                           [org.clojure/clojurescript "1.7.48"]
                           [com.quipucamayoc/carta "0.5.3"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

            :npm {:dependencies [[source-map-support "0.3.2"]
                                 [osc-min "0.2.0"]
                                 [noble "1.0.2"]]}

            :plugins [[lein-cljsbuild "1.0.6"]
                      [lein-ancient "0.6.7"]
                      [lein-cljfmt "0.3.0"]
                      [lein-marginalia "0.8.0"]
                      [lein-npm "0.6.1"]]

            :source-paths ["src"]

            :main "run/connect.js"

            :clean-targets ["run/out" "run/connect.js" "run/connect.js.map"]

            :cljsbuild {
                        :builds [{:id           "core"
                                  :source-paths ["src"]
                                  :compiler     {:source-map    "run/connect.js.map"
                                                 :output-to     "run/connect.js"
                                                 :output-dir    "run/out"
                                                 :target        :nodejs
                                                 :optimizations :none
                                                 :main          device-server.device
                                                 :pretty-print  true}}]})
