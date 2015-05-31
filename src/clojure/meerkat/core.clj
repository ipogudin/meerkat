(ns meerkat.core
  (:require [meerkat.httpservice.netty :as httpservice]
            [meerkat.httpservice.default-handler :as handlers]
            [meerkat.httpservice.routing :as routing]))

(defn configure-routing []
  (->
    (routing/register-default-handler handlers/default-handler)
    (routing/build-router)))

(defn start-http-service []
  (let [service (httpservice/start {})]
    (httpservice/set-router service (configure-routing))
    service))

(defn stop-http-service [service]
  (httpservice/stop service))