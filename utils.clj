(ns utils
  (:require [clojure.set :refer [subset?]])
  (:gen-class))

(defmacro zip [& lists]
  `(map vector ~@lists))

(defmacro submap? [submap supermap]
  `(subset? (set ~submap) (set ~supermap)))

(defn every-submap? [submaps supermaps]
  (if (not= (count submaps) (count supermaps))
      false
      (every? (fn [[a b]] (submap? a b))
              (zip submaps supermaps))))
