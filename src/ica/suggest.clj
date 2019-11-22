(ns ica.suggest
  "a namespace that sets up the park suggestions"
  (:gen-class)
  (:require [clojure.java.io :as io]
            [cheshire.core :as cheshire :refer :all]
            [clojure.string :as string])
  (:use [ica.core :only (tokenize)])
  (:use [plural.core]))

(def recogs
  "It parses the strings from reg_phrases.json into a hashmap that is used to recognize certain keywords from the chat."
  (first (cheshire/parsed-seq (io/reader "src/ica/recog_phrases.json") true)))

(defn recognize [sentence]
  "It reads a string and checks if there is anything that matches the words in recog_phrases.json.
  It returns a list of matched words."
  (for [phrase (tokenize sentence)
        feature (keys recogs)
        :when (some #(when (or (= phrase %) (= phrase (pluralize %))) %) (get recogs feature))]
        phrase))
