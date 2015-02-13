(ns transit.csv
  "CSV (comma separated values) in/out"
  (:require
   [clojure.string :refer [join split split-lines]]))


(defn to-csv
  "Turn 2-dimensional collection into CSV string.
  Provide vector of column headings."
  [headings coll]
  (let [csv #(str (join "," %) "\r\n")]
	  (apply str (map csv (cons headings coll)))
	  ))


(defn parse-csv
  "Turn CSV string into collection of maps"
  [csv]
  (let [uncsv (fn [s] (split s #","))
		lines (split-lines csv)
		header (map keyword (uncsv (first lines)))]
	(map #(zipmap header (uncsv %)) (rest lines))
	))
