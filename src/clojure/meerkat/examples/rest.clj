(ns meerkat.examples.rest
  (:gen-class)
  (:require [meerkat.httpservice.default-handler :as handlers]
            [meerkat.httpservice.routing :as routing]
            [meerkat.services :as services]
            [meerkat.httpservice.core :as http-service]
            [meerkat.httpservice.netty :as netty-http-service]
            [meerkat.httpservice.keepalive :refer [keep-alive-provider]]))

(defn hello-handler [context]
  (let [response-body
        (.getBytes
          (format "Hello %s, from meerkat!" (get-in context [:request :parameters :name]))
          "UTF-8")]
    (handlers/respond context response-body "text/plain; charset=UTF-8"))
  ((:complete context)))

(defn message-handler [context]
  (let [parameters (get-in context [:request :parameters])
        response-body
        (.getBytes
          (format "Hello %s, from meerkat!\n%s" (:name parameters) (:message parameters))
          "UTF-8")]
    (handlers/respond context response-body "text/plain; charset=UTF-8"))
  ((:complete context)))

(def default-routes-configuration
  (->
    (routing/register-default-handler handlers/default-handler)
    (routing/register-handler :GET "/test/test1" hello-handler)
    (routing/register-handler :GET "/test/test1/test2" hello-handler)
    (routing/register-handler :GET "/hello/:name/greet" hello-handler)
    (routing/register-handler :GET "/hello/:name/message/:message/show" message-handler)))

(defn configure-routing [configuration]
  (routing/build-router configuration))

(defn start-http-service [router]
  (let [service (services/start (netty-http-service/create-http-service "http-service" []))]
    (http-service/set-router
      service
      (keep-alive-provider router (:read-timeout (deref (:configuration service)))))
    service))

(defn stop-http-service [service]
  (services/stop service))

(defn -main [& args]
  (start-http-service (configure-routing default-routes-configuration)))