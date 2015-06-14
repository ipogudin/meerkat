(ns meerkat.httpservice.netty
  (:require [clojure.string :as str])
  (:import 
    (meerkat.java.httpservice.netty HttpService)
    (meerkat.java.httpservice.netty Response)))

(defn- process-keep-alive-headers
  [context]
  (let [
        response (:response context)
        connection-header (str/lower-case (or (get-in context [:request :headers :connection]) ""))]
    (cond
      (= "keep-alive" connection-header)
      (-> context
        (assoc-in [:response :headers :connection] "keep-alive")
        (assoc-in [:response :headers :keep-alive] "timeout=60"))
      :else
      (-> context
        (assoc-in [:response :headers :connection] "close")
        (assoc :complete (fn []
                           ((:complete context))
                           ((:close context))))))))

(defn keep-alive-provider
  [router]
  (fn [context]
    (router
      (process-keep-alive-headers context))))

(defn set-router
  [service router]
  (.setRouter service 
    (keep-alive-provider router)))

(defn stop
  "Stop "
  [service]
  (.stop service))

(defn start
  "Start "
  [configuration]
  (doto 
    (HttpService. 
      (merge {
              :ssl false
              :port 8080
              :acceptor-pool-size 8
              :worker-pool-size 8
              :backlog 1024
              } configuration))
    (.start)))
