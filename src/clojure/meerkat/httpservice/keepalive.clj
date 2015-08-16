(ns meerkat.httpservice.keepalive
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn- process-keep-alive-headers
  [context timeout-header-value]
  (log/trace "keep-alive processing")
  (let [
        response (:response context)
        protocol (get-in context [:request :protocol])
        connection-header (str/lower-case (or (get-in context [:request :headers :connection]) ""))]
    (cond
      (or (= protocol "HTTP/1.1") (= "keep-alive" connection-header))
      (-> context
        (assoc-in [:response :headers :connection] "keep-alive")
        (assoc-in [:response :headers :keep-alive] timeout-header-value))
      :else
      (-> context
        (assoc-in [:response :headers :connection] "close")
        (assoc :complete (fn []
                           (apply (:complete context) [])
                           (apply (:close context) [])))))))

(defn keep-alive-provider
  "timeout - keep-alive timeout in milliseconds 
(Remember that keep-alive-provider doesn't controll timeout for the connection. It just sets proper http headers.)"
  [router timeout]
  (let [timeout-header-value (format "timeout=%d" (quot timeout 1000))]
    (fn [context]
      (router
        (process-keep-alive-headers context timeout-header-value)))))