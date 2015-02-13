(ns transit.ptv
  "Make calls to PTV API"
  (:import (javax.crypto Mac)
		   (javax.crypto.spec SecretKeySpec))
  (:require
   [clojure.data.json :as json]
   [transit.auth :refer [DEVID DEVKEY]]
   ))


; API web site
(def BASEURL "http://timetableapi.ptv.vic.gov.au")


; PTV transport modes
(def METROTRAIN 0)
(def TRAM       1)
(def BUS        2)  ; metro and regional, not V/Line
(def VLINE      3)  ; train and coach
(def NIGHTRIDER 4)


(defn- signature [key msg]
  "Returns SHA1 HMAC (hex string) of msg with a given key"
  (let [mac (Mac/getInstance "HMACSHA1")
		secret (SecretKeySpec. (.getBytes key) (.getAlgorithm mac))]
	(->>
	 (doto mac
	   (.init secret)
	   (.update (.getBytes msg)))
	 .doFinal
	 (map #(format "%02X" %))
	 (apply str)
	 )))


(defn- query-ptv
  "Sign 'method', send to PTV and process JSON response"
  [method]
  (-> (str BASEURL method "&signature=" (signature DEVKEY method))
	  slurp
	  (json/read-str :key-fn keyword)
	  ))


(defn fetch-points-of-interest
  "Fetch points of interest from PTV API within geographic area"
  [mode lat1 lng1 lat2 lng2 griddepth limit]
  (:locations (query-ptv (str "/v2/poi/" mode
							  "/lat1/" lat1 "/long1/" lng1 "/lat2/" lat2 "/long2/" lng2
							  "/griddepth/" griddepth "/limit/" limit "?devid=" DEVID))))


(defn fetch-stops-on-line
  "Fetch stops for given route (which PTV calls a line)"
  [mode line]
  (query-ptv (str "/v2/mode/" mode "/line/" line
				  "/stops-for-line?devid=" DEVID)))


(defn fetch-broad-next-departures
  "Fetch departures from given stop"
  [mode stop limit]
  (query-ptv (str "/v2/mode/" mode "/stop/" stop
				  "/departures/by-destination/limit/" limit "?devid=" DEVID)))


(defn fetch-specific-next-departures
  "Fetch departures from given stop/line"
  [mode line stop directionid limit]
  (query-ptv (str "/v2/mode/" mode "/line/" line "/stop/" stop
				  "/directionid/" directionid "/departures/all/limit/" limit
				  "?devid=" DEVID)))


(defn fetch-stopping-pattern
  "Fetch stopping pattern for given stop/run
  utc is in ISO8601 format e.g. 2013-11-13T05:24:25Z"
  [mode run stop utc]
  (query-ptv (str "/v2/mode/" mode "/run/" run
				  "/stop/" stop "/stopping-pattern?for_utc=" utc "&devid=" DEVID)))


(defn stop-to-gtfs
  "Convert stop from PTV API to GTFS format (id, name, lat, lng)"
  [ptv-stop]
  (vector (:stop_id ptv-stop)
		  (str (:location_name ptv-stop) "(" (:suburb ptv-stop) ")")
		  (:lat ptv-stop)
		  (:lon ptv-stop)))


; avoid fetching same data twice
(def cached-broad-next-departures (memoize fetch-broad-next-departures))


(defn fetch-stop-run-ids
  "Fetch all run IDs for given stop id"
  [stop]
  (->> (cached-broad-next-departures BUS stop 0)
	   :values
	   (map #(get-in % [:run :run_id]))
	   set))


(defn fetch-line-run-ids
  "Fetch all run IDs for given line id"
  [line]
  (->> (fetch-stops-on-line BUS line)
	   (map :stop_id)
	   (mapcat fetch-stop-run-ids)
	   set))


(defn fetch-lines
  "Queries stop for its next 50 departures, returns line (route) ids and names accordingly."
  [stop-id]
  (let [limit 50
		result (fetch-broad-next-departures BUS stop-id limit)
		f #(-> % :platform :direction :line (select-keys [:line_id :line_name]))]
	(set (map f (:values result)))))

