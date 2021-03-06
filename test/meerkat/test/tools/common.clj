(ns meerkat.test.tools.common
  (:import [java.util.concurrent LinkedBlockingQueue]))

(def ^:dynamic *recorded-values* (LinkedBlockingQueue.))

(defn record [value]
  "Record value in the global storage"
  (.put *recorded-values* value))

(defn get-recorded []
  "Get one value from the global storage. It blocks during 1000ms to wait for a value."
  (.poll *recorded-values* 1000 java.util.concurrent.TimeUnit/MILLISECONDS))

(defn clear-recorder []
  (.clear *recorded-values*))