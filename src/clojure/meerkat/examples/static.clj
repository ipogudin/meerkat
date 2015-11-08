(ns meerkat.examples.static
  (:gen-class)
  (:require [meerkat.httpservice.default-handler :as handlers]
            [meerkat.httpservice.routing :as routing]
            [meerkat.services :as services]
            [meerkat.httpservice.core :as http-service]
            [meerkat.httpservice.netty :as netty-http-service]
            [meerkat.httpservice.keepalive :refer [keep-alive-provider]]
            [meerkat.httpservice.static :as static]))

(defn -main [& args]
  (let [services [(netty-http-service/create-http-service :http-service [])
                  (static/create-static-service :static-service [])]
        configuration {:http-service {}
                       :static-service {:url-prefix "/static"}}
        sorted-services (services/sort-services services)
        services-with-dependencies (services/inject-dependencies sorted-services)
        _ (services/configure-all services-with-dependencies configuration)
        http-service-i (first (filter #(= :http-service (:name %)) services-with-dependencies))
        static-service-i (first (filter #(= :static-service (:name %)) services-with-dependencies))
        router (routing/build-router (->
                                       (routing/register-default-handler handlers/default-handler)
                                       (routing/register-handler
                                         :GET
                                         (str (-> static-service-i :configuration deref :url-prefix) ":resource")
                                         (partial http-service/handle static-service-i))))]
    (services/start-all services-with-dependencies)
    (http-service/set-router
      http-service-i
      (keep-alive-provider router (:read-timeout (deref (:configuration http-service-i)))))))