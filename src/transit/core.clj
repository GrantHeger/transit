(ns transit.core
  (:require
   [transit.ptv :as ptv]
   [transit.csv :as csv]
   [transit.gtfs :as gtfs]
   [clojure.pprint :refer [pprint]]
   ))


(defn ballarat-stops
  []
  (let [north   -37.511713
		west      143.779905
		south     -37.638240
		east      143.923586
		griddepth 1
		limit     600]
	(ptv/fetch-points-of-interest
	 ptv/BUS north west south east griddepth limit)
	))


(def ballarat-routes [
					  [5499	18	"Ballarat - Alfredton (Route 18)"]
					  [5486	5	"Ballarat - Black Hill (Route 5)"]
					  [7855	7	"Ballarat - Brown Hill (Route 7)"]
					  [5491	10	"Ballarat - Buninyong (Route 10)"]
					  [7870	9	"Ballarat - Canadian (Route 9)"]
					  [5484	3	"Ballarat - Creswick (Route 3)"]
					  [5494	13	"Ballarat - Delacombe via Pleasant Street (Route 13)"]
					  [5495	14	"Ballarat - Delacombe via Sutton Street (Route 14)"]
					  [7867	8	"Ballarat - Eureka (Route 8)"]
					  [5726	4	"Ballarat - Invermay (Route 4)"]
					  [5497	16	"Ballarat - Lake Gardens (Route 16)"]
					  [7876	17	"Ballarat - Miners Rest (Route 17)"]
					  [7873	11	"Ballarat - Mount Pleasant (Route 11)"]
					  [5493	12	"Ballarat - Sebastopol (Route 12)"]
					  [5496	15	"Ballarat - Sturt Street West (Route 15)"]
					  [7864	6	"Ballarat - Webbcona (Route 6)"]
					  [5483	2	"Ballarat - Wendouree (Route 2)"]
					  [6720	1	"Ballarat - Wendouree West (Route 1)"]
					  [5500	19	"Delacombe - Sebastopol (Route 19)"]
					  ])


(def days ["MON" "TUE" "WED" "THU" "FRI" "SAT" "SUN"])


(defn make-stops-csv
  "Build Ballarat GTFS stops file from PTV data"
  []
  (->> (ballarat-stops)
	   (map ptv/stop-to-gtfs)
	   (map csv/to-csv)
	   (apply str)
	   (str "stop_id,stop_name,stop_lat,stop_lng\r\n")
	   ))


(defn make-routes-csv
  "Build Ballarat GTFS routes file from static data"
  []
  (->> ballarat-routes
	   (map #(conj % gtfs/BUS))
	   (map csv/to-csv)
	   (apply str)
	   (str "route_id,route_short_name,route_long_name,route_type\r\n")
	   ))


(defn make-calendar-csv
  "Build GTFS calendar file, one day per service (might as well just
  output the raw csv!"
  []
  (let [zeroes (vec (repeat 7 0))
		start "20140101"
		end "20180101"]
	(->> (for [n (range 7)]
		   [(get days n) (assoc zeroes n 1) start end])
		 (map flatten)
		 (map csv/to-csv)
		 (apply str)
		 (str "service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\r\n")
		 )))


(defn main
  []
  (println "Creating feed for Ballarat bus stops")
  (println "Creating" "feed/stops.txt")
  (spit "feed/stops.txt" (make-stops-csv))
  ;(println "Creating" "feed/routes.txt")
  ;(spit "feed/routes.txt" (make-routes-csv))
  (println "Done")
  )

