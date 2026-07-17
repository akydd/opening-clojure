(ns opening-clojure.song
  (:require [overtone.live :refer [definst saw square env-gen perc adsr FREE line:kr] :as overtone]
            [leipzig.melody :refer [tempo bpm where with phrase then times]]
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

(defn phrase-maker [pairs duration]
  (->>
   (phrase (repeat (* duration (count pairs)) (/ 1 duration))
           (reduce
            (fn [acc notes]
              (into acc (take duration (cycle notes))))
            [] pairs))))

; `notes` arrives as a lazy seq (phrase/then/times/with all return seqs), and
; update-in needs an Associative coll -- on a seq (get notes i) yields nil, so
; (dec nil) would NPE. vec first, then lower the second-to-last note's pitch.
(defn descend [notes]
  (let [v (vec notes)]
    (update-in v [(- (count v) 2) :pitch] dec)))

(def top-a
  (->>
   (times 3 (descend (phrase-maker [[0 2] [4 0] [1 4] [2 4]] 12)))
   (then (phrase-maker [[0 2] [4 0] [1 4] [1 4]] 12))))

(def top-b
  (->>
   (times 3 (phrase-maker [[-3 0] [-2 0] [-4 -1] [-3 -1]] 12))
   (then (phrase-maker [[-3 0] [-2 0] [-4 -1] [-4 -1]] 12))))

(def top-c
  (->>
   (times 3 (phrase-maker [[5 0] [2 5] [6 2] [6 2]] 12))
   (then (phrase-maker [[5 1] [5 1] [5 2] [5 3]] 12))))

(def mid-a
  (->>
   (times 4 (phrase-maker (concat
                           (repeat 2 [-5 -3])
                           (repeat 2 [-6 -4]))
                          8))))

(def mid-b
  (->>
   (times 4 (phrase-maker (concat
                           (repeat 2 [-7 -5])
                           (repeat 2 [-8 -6]))
                          8))))

(def mid-c
  (->>
   (times 3 (phrase-maker (concat
                           (repeat 2 [-4 -2])
                           (repeat 2 [-3 -1]))
                          8))
   (then (phrase-maker (repeat 4 [-4 -2]) 8))))

(def track
  (->>
   (with top-a mid-a)
   (then (with top-b mid-b))
   (then (with top-c mid-c))
   (where :pitch (comp temperament/equal scale/F scale/dorian))
   (tempo (bpm 30))))
