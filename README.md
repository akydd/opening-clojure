# opening-clojure

A Clojure rendering of **"Opening"**, the first movement of Philip Glass's
*Glassworks* (1982), written using [Overtone](https://overtone.github.io/) (for
sound synthesis) and [Leipzig](https://github.com/ctford/leipzig) (for
composition). The whole piece lives in `src/opening_clojure/song.clj` as a value
called `track`. Play it straight from the command line with `lein run`, or
evaluate it at a REPL.

## About the piece

*Glassworks* is a six-movement chamber work by [Philip
Glass](https://en.wikipedia.org/wiki/Philip_Glass), composed in 1981 and
recorded in 1982 for CBS Masterworks. "Opening" is its first movement — a solo
piano piece built on Glass's trademark contrary motion, with the left hand
playing quarter notes against triplets in the right. This project is a
transcription of that movement for Overtone and Leipzig; it is not an official
or affiliated release, and all rights to the composition remain with its
copyright holders.

See [Glassworks (composition)](https://en.wikipedia.org/wiki/Glassworks_(composition))
on Wikipedia for more background.

## Prerequisites

### SuperCollider

Overtone makes sound through [SuperCollider](https://supercollider.github.io/)'s
synthesis server (`scsynth`). You must install it before anything will play.

On macOS with [Homebrew](https://brew.sh/):

```sh
brew install --cask supercollider
```

Overtone starts and talks to the server automatically — you do **not** need to
open the SuperCollider IDE. You just need it installed so `scsynth` is on disk.

### A JVM build tool

You need either [Leiningen](https://leiningen.org/) or the
[Clojure CLI](https://clojure.org/guides/install_clojure) (`clj`/`deps.edn`).
Both are set up in this project (`project.clj` and `deps.edn`).

```sh
brew install leiningen        # for the lein workflow
brew install clojure/tools/clojure   # for the deps.edn / CLI workflow
```

## Installing dependencies

With Leiningen, dependencies are fetched the first time you start a REPL, or
explicitly with:

```sh
lein deps
```

With the Clojure CLI, they download on first use, or explicitly with:

```sh
clj -P
```

## Playing the music

The piece is played by evaluating `track` with Leipzig's `live/play`. You can
either run it directly from the command line or drive it interactively from a
REPL.

### With `lein run`

```sh
lein run
```

`-main` (in `src/opening_clojure/song.clj`) boots Overtone, plays the piece, and
blocks until it finishes before exiting. Because `live/play` schedules the notes
on a background thread and returns immediately, `-main` sleeps for the length of
the piece (plus a short tail) so the JVM doesn't quit before the music plays.
Press `Ctrl-C` to stop early.

### With a Leiningen REPL

Start a REPL:

```sh
lein repl
```

The first start takes a while: it pulls dependencies, boots the JVM, and starts
the SuperCollider server (you'll see Overtone's boot messages). Then, at the
prompt:

```clojure
;; Load the song namespace (this also boots Overtone via overtone.live).
(require '[opening-clojure.song :as song])

;; Play the piece.
(leipzig.live/play song/track)
```

Or, if you'd rather work from inside the namespace:

```clojure
(in-ns 'opening-clojure.song)
(live/play track)
```

To stop all sound immediately:

```clojure
(overtone.live/stop)
```

### With Emacs + CIDER

1. Open `src/opening_clojure/song.clj` in Emacs.
2. Start a CIDER REPL connected to the project with `M-x cider-jack-in`
   (`C-c C-x j j`). CIDER detects `project.clj` and uses Leiningen. Wait for
   Overtone/SuperCollider to finish booting in the REPL buffer.
3. Load the buffer with `C-c C-k` (`cider-load-buffer`). This evaluates the
   whole file, defining `track` and the instruments.
4. Put the cursor at the end of the `(def track ...)` form and evaluate it with
   `C-c C-e` (`cider-eval-last-sexp`) if it isn't defined yet.
5. In the REPL (or by typing the form in the buffer and pressing `C-c C-e`),
   play the piece:

   ```clojure
   (live/play track)
   ```

   `live` is already aliased in the namespace, so no extra require is needed
   once the buffer is loaded.

To stop playback:

```clojure
(overtone/stop)
```

(`overtone` is the alias for `overtone.live` in this namespace.)

## Troubleshooting

- **No sound / server won't boot:** confirm SuperCollider is installed and that
  `scsynth` is on your `PATH`. Check your system output device and volume.
- **Long first boot:** the initial REPL start downloads dependencies and boots
  SuperCollider; subsequent starts are faster.
- **`lein run` exits without sound:** make sure SuperCollider is installed; the
  server must boot before `-main` can play anything.
