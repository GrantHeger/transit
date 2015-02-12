(ns transit.csv
    "CSV (comma separated values) in/out"
    (:require 
        [clojure.string :refer [join split split-lines]]))

(defn to-csv
    "Turn vector into CSV string"
    [vect]
    (str (join "," vect) "\r\n"))

(defn parse-csv
    "Turn CSV string into collect of maps"
    [csv]
    (let [uncsv (fn [s] (split s #","))
          lines (split-lines csv)
          header (map keyword (uncsv (first lines)))]
        (map #(zipmap header (uncsv %)) (rest lines))
        ))
