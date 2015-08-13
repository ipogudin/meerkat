(ns meerkat.httpservice.netty
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [meerkat.httpservice.core :refer :all]
            [meerkat.services :as services])
  (:import 
    [meerkat.java.httpservice.netty HttpServiceImpl]
    [meerkat.services Service]))

(defn- process-keep-alive-headers
  [context]
  (log/trace "keep-alive processing")
  (let [
        response (:response context)
        protocol (get-in context [:request :protocol])
        connection-header (str/lower-case (or (get-in context [:request :headers :connection]) ""))]
    (cond
      (or (= protocol "HTTP/1.1") (= "keep-alive" connection-header))
      (-> context
        (assoc-in [:response :headers :connection] "keep-alive")
        (assoc-in [:response :headers :keep-alive] "timeout=60"))
      :else
      (-> context
        (assoc-in [:response :headers :connection] "close")
        (assoc :complete (fn []
                           (apply (:complete context) [])
                           (apply (:close context) [])))))))

(defn keep-alive-provider
  [router]
  (fn [context]
    (router
      (process-keep-alive-headers context))))

(defrecord NettyHttpService [name dependencies configuration netty-service]
  meerkat.services.Service
  (start [this]
    (let 
      [service
        (doto 
          (HttpServiceImpl. 
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
                    } @configuration))
          (.start))]
      (log/info "http-service just started")
      (swap! 
        netty-service 
        (fn [old-netty-service] 
          (if old-netty-service
            (.stop old-netty-service))
          service))
      this))
  (stop [this]
    (.stop @netty-service)
    (log/info "http-service just stopped"))
  (configure [this new-configuration] 
    (reset! configuration new-configuration))
  HttpService
  (set-router [this router]
    (.setRouter @netty-service
      (keep-alive-provider router))))

(defn create-http-service
  [name dependencies configuration]
  (NettyHttpService. name dependencies (atom configuration) (atom nil)))
