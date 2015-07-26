(ns meerkat.servicemanagement.core
  (:require [clojure.set]))

(defprotocol Service
  (configure [this new-configuration] "Configure service. To apply coniguration you should stop and start service.")
  (start [this] "start service")
  (stop [this] "stop service"))

(defn- set-max-iterations
  "Set maximum iterations for topological sorting"
  [s]
  (let [l (count s)]
  (mapv #(with-meta % (assoc (meta %) :max-iterations (* l l))) s)))

(defn- dec-and-check-max-iterations
  "Decrement and check if iterations are exhausted"
  [o]
  (if (nil? o)
    o
    (let [
          m (update (meta o) :max-iterations dec)]
      (if (< 0 (:max-iterations m))
        (with-meta o m)
        (throw (RuntimeException. (format "Unsatisfied or cyclic dependencies for %s" o)))))))

(defn sort-topologically [[unckecked-service & services] reviewed sorted]
  (let
    [service (dec-and-check-max-iterations unckecked-service)]
    (cond
      (nil? service) (if 
                       (empty? reviewed) sorted
                       (recur reviewed [] sorted))
      (empty? (:dependencies service)) (recur services reviewed (conj sorted service))
      (clojure.set/subset? (set (:dependencies service)) (set (map :name sorted))) (recur services reviewed (conj sorted service))
      :else (recur services (conj reviewed service) sorted))))

(defn sort-services
  "Sort services according to dependencies"
  [services]
  (sort-topologically (set-max-iterations services) [] []))