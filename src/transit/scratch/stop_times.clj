(ns transit.stop-times
    (:require
        [clojure.string :as s]
        [transit.common :refer [to-csv parse-csv]]
        ))

(import '[org.jsoup Jsoup])

(defn fetch-trip-ids
    "Fetch trip ids from trips.txt"
    []
    (->> (slurp "trips.txt")
         parse-csv
         (map :trip_id)
         ))

(defn fetch-stop-ids
    "Fetch stop ids from stops.txt"
    []
    (->> (slurp "stops.txt")
         parse-csv
         (map :stop_id)
         ))

(defn splitter
    "Like split, but rearranged to take advantage of ->> macro"
    [regex str]
    (s/split str regex))

(defn replacer
    "Replace text matching pattern"
    [regex replace str]
    (s/replace str regex replace))

(defn convert-time
    "Convert to time format required by Google Transit.
    https://developers.google.com/transit/gtfs/reference#stop_times_fields.
    VERY limited input types, e.g.;
        8:45AM      08:45:00
        11:02PM     23:02:00
        99:99AM     nil"
    [input-time]
    (let [result (first (re-seq #"^(\d{1,2}):(\d\d)([AP]M)$" input-time))
          hour (Integer/parseInt (second result))
          minute (Integer/parseInt (nth result 2))
          ampm (last result)
          addhours (if (and (< hour 12) (= ampm "PM")) 12 0)]
        [hour minute ampm]
        (if (= 99 hour)
            nil
            (format "%02d:%02d:00" (+ addhours hour) minute)
            )))

(defn fetch-timetable
    "Given trip/line id, return timetable data from PTV website"
    [id]
    (let [html (slurp (str "http://ptv.vic.gov.au/timetables/line/" id 
                    "?command=direct&language=en&net=vic&" 
                    "line=0618B&sup=%20&project=ttb&outputFormat=0&" 
                    "itdLPxx_loadNTPs=1&itdLPxx_showNTPS=13&" 
                    "itdLPxx_selLineDir=R&itdLPxx_selWDType=T0&" 
                    "itdLPxx_scrollOffset=0"))
          stop-ids (map last (re-seq #"href=\"stop\/view\/(\d+)" html))
          times (->> html
                     (replacer #"<span>(\d{1,2}:\d{1,2})<\/span>" "$1AM")    ; identify AMs
                     (replacer #"<span>-<\/span>" "99:99AM")                 ; empty slots as 99:99AM
                     (replacer #"<b>(\d{1,2}:\d{1,2})<\/b>" "$1PM")          ; identify PMs
                     (re-seq #"\d{1,2}:\d{1,2}([AP]M)") (map first)          ; excise times
                     (map convert-time)                                      ; convert to Google Transit format
                     )
          cols (/ (count times) (count stop-ids))
          ptimes (for [n (range cols)] (take-nth cols (drop n times)))       ; group times by trip
          stops (map #(map vector (range) stop-ids %) ptimes)
          ]
        (for [stoplist stops]
            (remove #(nil? (last %)) stoplist)
            )))

(defn make
    []
    (fetch-timetable "7538")
    )
