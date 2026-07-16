(ns opening-clojure.song
  (:require [overtone.live :refer [definst saw square env-gen perc adsr FREE line:kr] :as overtone]
            [leipzig.melody :refer [tempo bpm where with phrase then all mapthen]]
            [leipzig.scale :as scale]
            [leipzig.live :as live]
            [leipzig.chord :as chord]
            [leipzig.temperament :as temperament]))

;; Work around leipzig 0.10.0: its private `trickle` calls (Thread/sleep <x>)
;; where x is a Double/Ratio. On Clojure 1.12 + modern Java, reflection won't
;; coerce those to the primitive long that Thread.sleep needs, so every note
;; after the first throws ("No matching method sleep found taking 1 args") and
;; the melody's future dies. Re-def the var to coerce the sleep value to long.
(alter-var-root
 #'live/trickle
 (constantly
  (fn trickle [[note & others]]
    (when-let [{epoch :time} note]
      (Thread/sleep (long (max 0 (- epoch (+ 100 (overtone/now))))))
      (cons note (lazy-seq (trickle others)))))))

; Instruments
(definst bass [freq 110 volume 1.0]
  (-> (saw freq)
      (* (env-gen (perc 0.1 0.4) :action FREE))
      (* volume)))

(defmethod live/play-note :default [{hertz :pitch}] (bass hertz))

(defn xin [pitch-one pitch-two duration]
  (->>
   (phrase (repeat duration (/ 1 duration)) (take duration (cycle [pitch-one pitch-two])))))

(def top
  (xin 0 2 12))

(def top-2
  (xin 4 0 12))

(def mid
  (xin -3 0 8))

(def track
  (->>
   top
   ;;mid
   (with mid)
   (then (with top-2 mid))
   (where :pitch (comp temperament/equal scale/F scale/dorian))
   (tempo (bpm 45))))
