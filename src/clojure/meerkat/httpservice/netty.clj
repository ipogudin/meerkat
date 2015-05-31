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

(defn- wrap-router
  [router]
  (fn [request ^meerkat.java.httpservice.netty.Response internal-response]
    (router 
      {
       :request request
       :write-and-flush (fn [context] 
                          (.write internal-response (:response context))
                          (.flush internal-response))
       :write (fn [context] (.write internal-response (:response context)))
       :flush #(.flush internal-response)
       :complete #(.complete internal-response)
       :close #(.close internal-response)
      })))

(defn set-router
  [service router]
  (.setRouter service 
    (wrap-router 
      (keep-alive-provider router))))

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
