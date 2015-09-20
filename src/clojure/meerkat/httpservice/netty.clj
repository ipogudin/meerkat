(ns meerkat.httpservice.netty
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [meerkat.httpservice.core :refer :all]
            [meerkat.common :as common]
            [meerkat.services :as services])
  (:import 
    [meerkat.java.httpservice.netty HttpServiceImpl]
    [meerkat.services Service]))

(def ^:dynamic *default-configuration* 
  {
    :ssl false
    :port 8080
    :acceptor-pool-size (common/get-available-processors)
    :worker-pool-size (common/get-available-processors)
    :backlog 1024
    :read-timeout 10000
    :write-timeout 10000
    :max-initial-line-length 4096
    :max-header-size 8192
    :max-chunk-size 8192})

(defrecord NettyHttpService [name dependencies configuration netty-service]
  Service
  (start [this]
    (let 
      [service
        (doto 
          (HttpServiceImpl. @configuration)
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
    (.setRouter @netty-service router)))

(defn create-http-service
  [name dependencies configuration]
  (NettyHttpService. 
    name 
    dependencies 
    (atom (merge *default-configuration* configuration)) 
    (atom nil)))
