(ns transit.core
  (:require
   [transit.ptv :as ptv]
   [transit.csv :as csv]
   [transit.gtfs :as gtfs]
   [clojure.pprint :refer [pprint]]
   ))


(def days ["MON" "TUE" "WED" "THU" "FRI" "SAT" "SUN"])


(def ballarat-stops
  (future
	(let [north	  -37.5117
		  west      143.7799
		  south     -37.6382
		  east      143.9235
		  griddepth 1
		  limit     600]
	  (ptv/fetch-points-of-interest ptv/BUS north west south east griddepth limit)
	  )))


(def routes
  (future (set (mapcat ptv/fetch-lines (map :stop_id @ballarat-stops)))))


(def trips
  (future
	(for [line (map :line_id @routes)
		  day days
		  run (ptv/fetch-runs-by-line line)]
	  [line day run])))


(defn make-stops-csv
  "Build Ballarat GTFS stops file from PTV data"
  [data]
  (->> data
	   (map ptv/stop-to-gtfs)
	   (csv/to-csv ["stop_id" "stop_name" "stop_lat" "stop_lng"])
	   ))


(defn make-routes-csv
  "Build Ballarat GTFS routes file from PTV data"
  [data]
  (let [f #(vector (:line_id %) (:line_name %) "" gtfs/BUS)]
	(->> data
		 (map f)
		 (csv/to-csv ["route_id" "route_short_name" "route_long_name" "route_type"])
		 )))


(defn make-calendar-csv
  "Build GTFS calendar file, one day per service.
  Might as well just output the raw csv!"
  []
  (let [zeroes (vec (repeat 7 0))
		start "20150101"
		end "20200101"]
	(->> (for [n (range 7)]
		   [(get days n) (assoc zeroes n 1) start end])
		 (map flatten)
		 (csv/to-csv ["service_id" "monday" "tuesday" "wednesday" "thursday" "friday" "saturday" "sunday" "start_date" "end_date"])
		 )))


(defn make-trips-csv
  [data]
  (csv/to-csv ["route_id" "service_id" "trip_id"] data))


(defn main
  []

  (println "Building feed for Ballarat bus stops")

  (println "Creating feed/stops.txt")
  (spit "feed/stops.txt" (make-stops-csv @ballarat-stops))

  (println "Creating feed/routes.txt")
  (spit "feed/routes.txt" (make-routes-csv @routes))

  (println "Creating feed/calendar.txt")
  (spit "feed/calendar.txt" (make-calendar-csv))

  (println "Creating feed/trips.txt")
  (spit "feed/trips.txt" (make-trips-csv @trips))

  (println "Done")
  )


(shutdown-agents) ; why?

