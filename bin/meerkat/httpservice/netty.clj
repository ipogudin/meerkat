(ns meerkat.httpservice.netty
  (:require [clojure.string :as str])
  (:import (meerkat.java.httpservice.netty HttpService)))

(defn- process-keep-alive-headers
  [request response]
  (cond
    (=
      "keep-alive"
      (str/lower-case (or (:connection (:headers request)) "")))
    (merge 
      response 
      {
       :headers (merge (:headers response) {:connection "keep-alive" :keep-alive "timeout=60"})
       :close #()
       })
    :else
      (merge 
        response 
        {
         :headers (assoc (:headers response) :connection "close")
         })))

(defn keep-alive-provider
  [router]
  (fn [request response]
    (router
      request
      (merge
        response
        {
         :write-and-flush (fn [r] ((:write-and-flush response) (process-keep-alive-headers request r)))
         :write (fn [r] ((:write response) (process-keep-alive-headers request r)))
         }))))

(defn- wrap-router
  [router]
  (fn [request internal-response]
    (router 
      request 
      {
       :write-and-flush (fn [response] 
                          (.write internal-response response)
                          (.flush internal-response))
       :write #(.write internal-response %)
       :flush #(.flush internal-response)
       :close #(.close internal-response)
      })))

(defn set-router
  [service router]
  (.setRouter service (wrap-router (keep-alive-provider router))))

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
          [request, response]
          (case (:method request)
            :GET (
                   (:write-and-flush response)
                   (let [response-body (.getBytes "Hello world, from meerkat!" "UTF-8")]
                     {
                      :body response-body
                      :headers {:content-type "text/plain" :content-length (count response-body)}
                      :status 200}))
            :POST (
                    (:write-and-flush response)
                    (let [response-body (:body request)]
                      {
                       :body response-body
                       :headers {:content-type "text/plain" :content-length (count response-body)}
                       :status 200}))
            (
              (:write-and-flush response) 
              (let [response-body (.getBytes "Method not found" "UTF-8")]
                {
                 :body response-body
                 :headers {:content-type "text/plain" :content-length (count response-body)}
                 :status 200})))
          ((:close response))
          ))
    (.start)))
