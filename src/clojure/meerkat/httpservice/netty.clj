(ns meerkat.httpservice.netty
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import 
    (meerkat.java.httpservice.netty HttpService)))

(defn- process-keep-alive-headers
  [context]
  (log/trace "keep-alive processing")
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
  (.stop service)
  (log/info "http-service just stopped")
  service)

(defn start
  "Start "
  [configuration]
  (let 
    [service
      (doto 
        (HttpService. 
          (merge {
                  :ssl false
                  :port 8080
                  :acceptor-pool-size 8
                  :worker-pool-size 8
                  :backlog 1024
                  :read-timeout 10000
                  :write-timeout 10000
                  :max-initial-line-length 4096
                  :max-header-size 8192
                  :max-chunk-size 8192
                  } configuration))
        (.start))]
    (log/info "http-service just started")
    service))
