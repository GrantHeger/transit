(ns transit.core
  (:require
   [transit.ptv :as ptv]
   [transit.csv :as csv]
   [transit.gtfs :as gtfs]
   [clojure.pprint :refer [pprint]]
   ))


(def ballarat-stops
  (let [north	  -37.511713
		west      143.779905
		south     -37.638240
		east      143.923586
		griddepth 1
		limit     600]
	(ptv/fetch-points-of-interest ptv/BUS north west south east griddepth limit)
	))


(def routes (mapcat ptv/fetch-lines (map :stop_id ballarat-stops)))


(defn make-stops-csv
  "Build Ballarat GTFS stops file from PTV data"
  []
  (->> ballarat-stops
	   (map ptv/stop-to-gtfs)
	   (csv/to-csv ["stop_id" "stop_name" "stop_lat" "stop_lng"])
	   ))


(defn make-routes-csv
  "Build Ballarat GTFS routes file from static data"
  []
  (let [f #(vector (:line_id %) (:line_name %) "" gtfs/BUS)]
	(->> (set routes)
		 (map f)
		 (csv/to-csv ["route_id" "route_short_name" "route_long_name" "route_type"])
		 )))


(defn make-calendar-csv
  "Build GTFS calendar file, one day per service.
  Might as well just output the raw csv!"
  []
  (let [days ["MON" "TUE" "WED" "THU" "FRI" "SAT" "SUN"]
		zeroes (vec (repeat 7 0))
		start "20150101"
		end "20200101"]
	(->> (for [n (range 7)]
		   [(get days n) (assoc zeroes n 1) start end])
		 (map flatten)
		 (csv/to-csv ["service_id" "monday" "tuesday" "wednesday" "thursday" "friday" "saturday" "sunday" "start_date" "end_date"])
		 )))


(defn main
  []
  (println "Building feed for Ballarat bus stops")
  (println "Creating" "feed/stops.txt")
  (spit "feed/stops.txt" (make-stops-csv))
  ;(println "Creating" "feed/routes.txt")
  ;(spit "feed/routes.txt" (make-routes-csv))
  (println "Creating" "feed/calendar.txt")
  (spit "feed/calendar.txt" (make-calendar-csv))
  (println "Done")
  )


(def headings ["service_id" "monday" "tuesday" "start_date"])

(clojure.string/join "," headings)


