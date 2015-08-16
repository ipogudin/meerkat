(ns meerkat.core
  (:require [meerkat.httpservice.default-handler :as handlers]
            [meerkat.httpservice.routing :as routing]
            [meerkat.services :as services]
            [meerkat.httpservice.core :as http-service]
            [meerkat.httpservice.netty :as netty-http-service]
            [meerkat.httpservice.keepalive :refer [keep-alive-provider]]))

(defn configure-routing []
  (->
    (routing/register-default-handler handlers/default-handler)
    (routing/build-router)))

(defn start-http-service []
  (let [service (services/start (netty-http-service/create-http-service "http-service" [] {}))]
    (http-service/set-router service (keep-alive-provider (configure-routing) (:read-timeout (deref (:configuration service)))))
    service))

(defn stop-http-service [service]
  (services/stop service))

(defn -main [& args]
  (start-http-service))