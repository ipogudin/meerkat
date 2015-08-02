(ns meerkat.services
  (:require [clojure.set])
  (:use [slingshot.slingshot :only [throw+ try+]]))

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
  "Decrement and check whether limit of iterations is exhausted"
  [o]
  (if (nil? o)
    o
    (let [
          m (update (meta o) :max-iterations dec)]
      (if (< 0 (:max-iterations m))
        (with-meta o m)
        (throw+ {:message (format "Unsatisfied or cyclic dependencies for %s" o)})))))

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

(defn inject-dependencies
  "Inject dependencies. Services should be topologically sorted according to dependencies."
  [services]
  (loop [
         [service & more] services
         services-map (into {} (mapv (fn [e] [(:name e) e]) services))
         services-with-dependencies []
         ]
    (let [
          service-with-dependencies 
          (reduce 
             (fn [service dependency] 
               (assoc service dependency (dependency services-map))) 
             service 
             (:dependencies service))
          ]
      (if (nil? service)
        services-with-dependencies
        (recur 
          more 
          (assoc services-map (:name service-with-dependencies) service-with-dependencies) 
          (conj services-with-dependencies service-with-dependencies))))))

(defn start-all
  [services]
  (mapv start services))

(defn stop-all
  [services]
  (mapv stop services))

(defn configure-all
  "Configure all services.
 This function invokes method configure for each service and pass appropriate sub-configuration.
 Configuration should contain sub-configuration according to service's name."
  [services configuration]
  (mapv (fn [service] (configure service ((:name service) configuration))) services))