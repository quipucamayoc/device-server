(defproject device-server "0.2.8"
            :description "The central communication node between wearable devices
  the rest of the project."
            :url "http://quipucamayoc.com/"

            :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                           [org.clojure/clojurescript "0.0-2985"]
                           [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

            :node-dependencies [[source-map-support "0.2.9"]
                                [osc-min "0.2.0"]
                                [noble "0.3.9"]
                                [blessed "0.0.49"]
                                [blessed-contrib "1.0.9"]]

            :plugins [[lein-cljsbuild "1.0.6-SNAPSHOT"]
                      [lein-ancient "0.6.3"]
                      [lein-cljfmt "0.1.10"]
                      [lein-marginalia "0.8.0"]
                      [lein-npm "0.5.0"]]

            :source-paths ["src"]

            :main "run/out/connect.js"

            :clean-targets ["run/out/connect" "run/connect.js" "run/connect.js.map"]

            :cljsbuild {
                        :builds [{:id           "galileo"
                                  :source-paths ["src"]
                                  :compiler     {:source-map    "run/connect.js.map"
                                                 :output-to     "run/connect.js"
                                                 :output-dir    "run/out"
                                                 :target        :nodejs
                                                 :optimizations :none
                                                 :language-in  :ecmascript5-strict
                                                 :language-out :ecmascript5-strict
                                                 :main          galileo.device
                                                 :pretty-print  true}}]})
