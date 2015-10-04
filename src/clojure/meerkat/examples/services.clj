(ns meerkat.examples.services
  (:gen-class)
  (:require [meerkat.services :as services])
  (:import [meerkat.services Service]))

(defprotocol Foo
  (foo [this] "Produce foo object."))

(defprotocol Renderer
  (render [this foo] "Render foo object."))

(defprotocol Printer
  (print-it [this] "Print a value."))

(defrecord FooService [name dependencies configuration]
  Service
  (configure [this new-configuration]
    (meerkat.services/configure-service this configuration new-configuration))
  (start [_] "start service" (println "FooService just started"))
  (stop [_] "stop service" (println "FooService just stopped"))
  Foo
  (foo [this] {:name (:name this) :value1 (-> this :configuration deref :value1)}))

(defrecord RendererService [name dependencies configuration]
  Service
  (configure [this new-configuration]
    (meerkat.services/configure-service this configuration new-configuration))
  (start [_] "start service" (println "RendererService just started"))
  (stop [_] "stop service" (println "RendererService just stopped"))
  Renderer
  (render [this foo] (format (-> this :configuration deref :format) (:name foo) (:value1 foo))))

; You should declare fields to be injected with dependencies.
; It allows to generate code with better performance.
(defrecord PrinterService [name dependencies configuration renderer foo]
  Service
  (configure [this new-configuration]
    (meerkat.services/configure-service this configuration new-configuration))
  (start [_] "start service" (println "PrinterService just started"))
  (stop [_] "stop service" (println "PrinterService just stopped"))
  Printer
  (print-it [_]
    (let [
          foo-result (meerkat.examples.services/foo foo)
          rendered-string (render renderer foo-result)]
      (println rendered-string))))

(defn -main [& args]
  (let [services [(->PrinterService :printer [:renderer :foo] (atom {}) nil nil)
                  (->RendererService :renderer [] (atom {}))
                  (->FooService :foo [] (atom {}))]
        configuration {:renderer {:format "Name of the service is %s and value1 is %s"}
                       :foo {:value1 356}}
        sorted-services (services/sort-services services)
        services-with-dependencies (services/inject-dependencies sorted-services)
        _ (services/configure-all services-with-dependencies configuration)
        foo-printer (first (filter #(= :printer (:name %)) services-with-dependencies))]
    (services/start-all services-with-dependencies)
    (print-it foo-printer)
    (services/stop-all (reverse services-with-dependencies))))
