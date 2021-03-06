(ns ica.core
  "the main namespace to prepare the chatbot"
  (:gen-class)
  (:require [clojure.java.io :as io]
            [cheshire.core :as cheshire :refer :all])
  (:use [ica.opennlp :only (tokenize)]
        [ica.parkData]
        [inflections.core])
  (:import [ica.parkData Park]))

(def bot-name "Guidia")

(def greetings
  "It contains a vector of greetings that are printed in the beginning of the chat."
  [(format "%s=> Hello, my name is %s, your guide to Prague parks." bot-name bot-name)
  (format "%s=> Tell me what you would like to see or like to do in Prague." bot-name)
  (format "%s=> I can suggest a park in Prague for you." bot-name)])

(def quitwords
  "It slurps a list of words from 'recog_phrases.json' that are used to quit chatbot's main loop."
  (get (first (cheshire/parsed-seq (io/reader "src/ica/recog_phrases.json") true)) :quitwords))

(defn word-exists? [quit-words sentence]
  "It takes a word or a sentence from the user and a list of quit words
  and iterates through them to find if they match in order to quit from the chatbot."
  (loop [lst quit-words]
    (when-not (empty? lst)
      (if (some #(when (= (first lst) %) %) (map clojure.string/lower-case (tokenize sentence)))
        true
        (recur (rest lst))))))

(defn data-comparer-helper-1 [position park-stored input-park]
  "It takes in two park records, a position in a record as a number,
  returns a boolean value if at the both values on the given position have the value of 'true'."
  (and
    (= true @((nth (rest (keys Bertramka)) position) input-park))
    (= true @((nth (rest (keys Bertramka)) position) park-stored))))

(defn data-comparer-helper-2 [park-stored input-park]
  "It takes in a list of parks, user inputted data and a parameter,
  adds the park to a locally stored vector if the chosen parameter in the park
  and user inputted data matches. Returns that vector of parks."
  (with-local-vars [counter 0]
    (doseq [position (range 7)]
      (if (data-comparer-helper-1 position park-stored input-park)
        (var-set counter (+ 1 @counter))))
    (var-get counter)))

(defn data-comparer-helper-3 [lst-park input-park]
  "It takes in a vector that contains records of all parks and the record that is created from user input."
  (with-local-vars [sim-vector []]
    (loop [lst-park-loc lst-park]
      (when-not (empty? lst-park-loc)
        (var-set sim-vector (conj @sim-vector (data-comparer-helper-2 (first lst-park-loc) input-park)))
        (recur (rest lst-park-loc))))
    (var-get sim-vector)))

(defn data-comparer-helper-4 [lst-park sim-vector highest]
  "It takes in a vector of all parks, a vector that contains similarity count  and the maximum from the vector,
  returns a vector that contain park record, that have the maximum similarity count in similarity count vector."
  (with-local-vars [park-matches []]
    (loop [position 0]
      (when (< position (count lst-park))
        (if (= (nth sim-vector position) highest)
          (var-set park-matches (conj @park-matches (nth lst-park position))))
        (recur (+ 1 position))))
    (var-get park-matches)))

(defn data-comparer-find-max [sim-vector]
  "It takes in a vector of numbers and returns the maximum."
  (with-local-vars [highest 0]
    (doseq [sim-counter sim-vector]
      (if (< @highest sim-counter)
        (var-set highest sim-counter)))
    (var-get highest)))

(defn data-comparer-main [lst-park input-park]
  "It takes in a vector of park records and a record, that was created from a user input and
  returns a vector of the best matched parks."
   (let* [sim-vector (data-comparer-helper-3 lst-park input-park)
          highest (data-comparer-find-max sim-vector)]
      (data-comparer-helper-4 lst-park sim-vector highest)))

(defn print-names [comparer-result]
  "It takes in a vector of parks and prints it in a sentence."
  (if (= 0 (count comparer-result))
    (println (format "%s=> Sorry. Nothing seems to match your preferences." bot-name))
    (if (= 1 (count comparer-result))
      (do
        (print (format "%s=> I would recommend " bot-name))
        (print (:name (first comparer-result)))
        (println "."))
      (do
        (print (format "%s=> I would recommend " bot-name))
        (loop [comparer-res comparer-result]
          (when-not (empty? comparer-res)
            (if (empty? (rest comparer-res))
              (do
              (print "and ")
              (print (:name (first comparer-res)))
              (println "."))
              (do
                (print (:name (first comparer-res)))
                (print ", ")))
            (recur (rest comparer-res))))))))

(defn greet []
  "It contains a procedural structure of a chatbot interface.
  It prints out greetings and (TODO: more contents)."
  (loop [grts greetings]
    (when-not (empty? grts)
      (doseq [timer (range (count greetings))]
        (Thread/sleep 500))
      (println (first grts))
      (recur (rest grts)))))

(defn interface [user-input]
  "It cascades other functions, get-userpark and data-comparer-main, and returns
  results of the park search in a single command. In doing so, it takes a string
  of the user input."
  (get-userpark user-input)
  (let* [matches (data-comparer-main lst-park user-park)]
    (print-names matches)))

(defn -main [& args]
  "It allows user to run the chatbot on command 'lein run'. It also loops the
  chatbot interface until a quitword is given. While it loops, it collects
  keywords from the user to find the user's preferences in parks. If the user
  says 'forget' instead, it resets the userpark to empty its data."
  (greet)
  (loop [user-input (do (print "User=> ") (flush) (read-line))]
    (when-not (word-exists? quitwords user-input)
      (if (= (clojure.string/lower-case user-input) "forget")
        (do
          (reset-userpark)
          (println (format "%s=> Your preferences have been cleared. Tell me about your new park." bot-name))
          (print "User=> "))
        (do
          (interface user-input)
          (println (format "%s=> If you want something more specific, tell me more what you wish.\n%s=> If you want me to forget your preferences, type 'forget'." bot-name bot-name))
          (print "User=> ")))
      (recur (do (flush) (read-line)))))
  (println (format "%s=> Bye!" bot-name)))
