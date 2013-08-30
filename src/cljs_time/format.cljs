(ns cljs-time.format
  (:require
    [cljs-time.core :as time]
    [clojure.string :as string]
    [goog.date :as date]))

(def months
  ["January" "February" "March" "April" "May" "June" "July" "August"
   "September" "October" "November" "December"])

(def days
  ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"])

(def date-formatters
  (let [d      #(.getDate %)
        M #(inc (.getMonth %))
        y      #(.getYear %)
        h      #(.getHours %)
        m      #(.getMinutes %)
        s      #(.getSeconds %)
        S      #(.getMilliseconds %)
        ]
    {"d" d
     "dd" #(format "%02d" (d %))
     "dth" #(let [d (d %)] (str d (case d 1 "st" 2 "nd" 3 "rd" "th")))
     "dow" #(days (.getDay %))
     "M" M
     "MM" #(format "%02d" (M %))
     "yyyy" y
     "yy" #(mod (y %) 100)
     "MMM" #(months (dec (M %)))
     "h" h
     "m" m
     "s" s
     "S" S
     "hh" #(format "%02d" (h %))
     "mm" #(format "%02d" (m %))
     "ss" #(format "%02d" (s %))
     "SSS" #(format "%03d" (S %))}))

(def date-parsers
  (let [y #(.setYear %1         (js/parseInt %2 10))
        d #(.setDate %1         (js/parseInt %2 10))
        M #(.setMonth %1   (dec (js/parseInt %2 10)))
        h #(.setHours %1        (js/parseInt %2 10))
        m #(.setMinutes %1      (js/parseInt %2 10))
        s #(.setSeconds %1      (js/parseInt %2 10))
        S #(.setMilliseconds %1 (js/parseInt %2 10))]
    {"d" ["(\\d{1,2})" d]
     "dd" ["(\\d{2})" d]
     "dth" ["(\\d{1,2})(?:st|nd|rd|th)" d]
     "M" ["(\\d{1,2})" M]
     "MM" ["(\\d{2})" M]
     "y" ["(\\d{1,4})" y]
     "yy" ["(\\d{2,4})" y]
     "yyyy" ["(\\d{4})" y]
     "MMM" [(str \( (string/join \| months) \))
            #(M %1 (str (inc (.indexOf (into-array months) %2))))]
     "dow" [(str \( (string/join \| days) \)) (constantly nil)]
     "h" ["(\\d{1,2})" h]
     "m" ["(\\d{1,2})" m]
     "s" ["(\\d{1,2})" s]
     "S" ["(\\d{1,2})" S]
     "hh" ["(\\d{2})" h]
     "mm" ["(\\d{2})" m]
     "ss" ["(\\d{2})" s]
     "SSS" ["(\\d{3})" S]}))

(defn parser-sort-order-pred [parser]
  (.indexOf
    (into-array ["yyyy" "yy" "y" "d" "dd" "dth" "M" "MM" "MMM" "dow" "h" "m"
                 "s" "S" "hh" "mm" "ss" "SSS"])
    parser))

(def date-format-pattern
  (re-pattern
    (str "(" (string/join ")|(" (reverse (sort-by count (keys date-formatters)))) ")")))

(defn date-parse-pattern [formatter]
  (re-pattern
    (string/replace formatter date-format-pattern #(first (date-parsers %)))))

(defn parse
  "Returns a DateTime instance in the UTC time zone obtained by parsing the
given string according to the given formatter."
  [s formatter]
  (reduce (fn [date [part do-parse]] #_(.log js/console part) (do-parse date part) date)
          (doto (date/DateTime.) (.set (js/Date. 0 0 0 0 0 0 0)))
          (map (fn [[a b]] [a (second (date-parsers b))])
               (sort-by (comp parser-sort-order-pred second)
                        (partition 2
                                   (interleave
                                     (nfirst (re-seq (date-parse-pattern formatter) s))
                                     (map first (re-seq date-format-pattern formatter))))))))

(defn unparse
  "Returns a string representing the given DateTime instance in UTC and in the
form determined by the given formatter."
  [date formatter]
  {:pre [(not (nil? date)) (instance? date/DateTime date)]}
  (string/replace formatter date-format-pattern #((date-formatters %) date)))