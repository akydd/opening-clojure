(defproject opening-clojure "0.1.0-SNAPSHOT"
  :main ^{:skip-aot true} opening-clojure.song
  :jvm-opts ^:replace []
  :dependencies [[org.clojure/clojure "1.12.5"]
                 [overtone "0.16.3331"]
                 [leipzig "0.10.0"]])
