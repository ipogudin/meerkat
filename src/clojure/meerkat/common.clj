(ns meerkat.common
  (:import [java.net URLDecoder]))

(defn decode-url
  ([url] (decode-url url "UTF-8"))
  ([url encoding] (URLDecoder/decode url encoding)))

(defn get-available-processors
  "Returns a number of available processors (including HT)"
  []
  (-> (Runtime/getRuntime) .availableProcessors long))