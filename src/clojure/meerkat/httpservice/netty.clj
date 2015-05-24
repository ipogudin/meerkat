(ns meerkat.httpservice.netty
  (:require [clojure.string :as str])
  (:import (meerkat.java.httpservice.netty HttpService)))

(defn- process-keep-alive-headers
  [context]
  (let [
        response (:response context)
        connection-header (str/lower-case (or (get-in context [:request :headers :connection]) ""))]
    (cond
      (= "keep-alive" connection-header)
      (-> context
        (assoc-in [:response :headers :connection] "keep-alive")
        (assoc-in [:response :headers :keep-alive] "timeout=60")
        (assoc :complete (fn []
                           ((:complete context))
                           ((:close context)))))
      :else
      (-> context
        (assoc-in [:response :headers :connection] "close")))))

(defn keep-alive-provider
  [router]
  (fn [context]
    (router
      (process-keep-alive-headers context))))

(defn- wrap-router
  [router]
  (fn [request internal-response]
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
    (set-router 
        (fn
          [context]
          (case (get-in context [:request :method])
            :GET (
                   (:write-and-flush context)
                   (let [response-body (.getBytes "Hello world, from meerkat!" "UTF-8")]
                     (-> context
                       (assoc-in [:response :body] response-body)
                       (assoc-in [:response :headers :content-type] "text/plain")
                       (assoc-in [:response :headers :content-length] (count response-body))
                       (assoc-in [:response :status] 200))))
            :POST (
                    (:write-and-flush context)
                    (let [response-body (get-in context [:request :body])]
                      (-> context
                       (assoc-in [:response :body] response-body)
                       (assoc-in [:response :headers :content-type] "text/plain")
                       (assoc-in [:response :headers :content-length] (count response-body))
                       (assoc-in [:response :status] 200))))
            (
              (:write-and-flush context) 
              (let [response-body (.getBytes "Method not found" "UTF-8")]
                (-> context
                       (assoc-in [:response :body] response-body)
                       (assoc-in [:response :headers :content-type] "text/plain")
                       (assoc-in [:response :headers :content-length] (count response-body))
                       (assoc-in [:response :status] 200)))))
          ((:complete context))))
    (.start)))
