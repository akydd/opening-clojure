(ns opening-clojure.song
  (:require [overtone.live :refer [definst env-gen perc FREE sin-osc] :as overtone]
            [leipzig.melody :refer [tempo bpm where with phrase then times]]
            [leipzig.scale :as scale]
            [leipzig.live :as live]
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

; Define a synth-piano instrument.
(definst synth-piano [freq 440 dur 2.0]
  (let [env (env-gen (perc 0.01 dur) :action FREE)
        ;; Fundamental + a few partials (overtones) characteristic of a struck string/bell
        sig (+ (* 0.5 (sin-osc freq))
               (* 0.25 (sin-osc (* freq 2.0)))
               (* 0.125 (sin-osc (* freq 3.0))))]
    (* env sig)))

(defmethod live/play-note :default [{hertz :pitch duration :duration}] (synth-piano hertz duration))

(defn phrase-maker
  "Creates a phrase of oscillating notes of equal `duration`.

  Each pair in `pairs` gets a single measure."
  [pairs duration]
  (->>
   (phrase (repeat (* duration (count pairs)) (/ 1 duration))
           (reduce
            (fn [acc notes]
              (into acc (take duration (cycle notes))))
            [] pairs))))

(defn descend
  "Drops the 2nd to last note in `notes` by a single pitch."
  [notes]
  (let [v (vec notes)]
    (update-in v [(- (count v) 2) :pitch] dec)))

(defn extend-duration
  "Extend the last note in `notes` by a given `duration`."
  [notes duration]
  (let [v (vec notes)]
    (update-in v [(dec (count v)) :duration] #(+ % duration))))

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

(def bass-a
  (->>
   (times 3 (phrase [2 1 1] [-7 -8 (scale/flat -9)]))
   (then (phrase [2 1 1] [-7 -8 -11]))))

(def bass-b
  (->>
   (times 4 (phrase [1 1 1 1] [-10 -11 -11 -13]))))

(def bass-c
  (->>
   (times 2 (phrase [2 1 1] [-7 -5 -4]))
   (then (phrase [2 1 1/2 1/2] [-7 -5 -4 -5]))
   (then (phrase [2 2] [-6 -7]))))

(def track
  (->>
   (with top-a mid-a bass-a)
   (then (with top-b mid-b bass-b))
   (then (with top-c mid-c bass-c))
   (times 2)
   (then (with top-a mid-a bass-a))
   (then (with top-b mid-b bass-b))
   (then (apply with (map #(extend-duration % 1) [top-c mid-c bass-c])))
   (where :pitch (comp temperament/equal scale/F scale/dorian))
   (tempo (bpm 30))))

(defn track-length
  "Compute the length, in seconds, of a track."
  [track]
  (reduce (fn [acc {:keys [time duration]}]
            (max acc (+ time duration)))
          0 track))

(defn -main
  "Entry point for `lein run`: play the piece and block until it finishes.

  `live/play` is asynchronous and returns immediately, so without blocking here
  the JVM would exit before any sound came out. After `(tempo (bpm 30))` each
  note's :time and :duration are in seconds, so the end of the piece is the
  greatest (:time + :duration)."
  [& _args]
  (live/play track)
  (let [length-secs (track-length track)]
    ;; A little tail padding so the final note's envelope can ring out.
    (Thread/sleep (long (* 1000 (+ 2 length-secs)))))
  (live/stop)
  (shutdown-agents)
  (System/exit 0))

(defn record-song
  "Save the song as a WAV."
  [filename]
  (overtone/recording-start (str filename ".wav"))
  (live/play track)
  ;; sleep for the song.
  (let [length-secs (track-length track)]
    (Thread/sleep (long (* 1000 (+ 3 length-secs)))))
  (overtone/recording-stop))
