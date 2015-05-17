(ns meerkat.core
  (:require [meerkat.httpservice.netty :as httpservice]))

(defn run-http-service [timeout]
  (let [service (httpservice/start {})]
    (java.lang.Thread/sleep timeout)
    (httpservice/stop service)))