(ns meerkat.servicemanagement.core_test
  (:require [clojure.test :refer :all]
            [meerkat.servicemanagement.core :as sm])
  (:import [meerkat.servicemanagement.core Service]))

(defrecord TestService [name dependencies configuration]
  meerkat.servicemanagement.core.Service
  (configure [this new-configuration] (swap! configuration (fn [_] new-configuration)))
  (start [this] "start service" (println "start"))
  (stop [this] "stop service" (println "stop")))

(defn- create-service [{name :name dependencies :dependencies configuration :configuration}] 
  (TestService. name dependencies configuration))

(deftest sorting-services-according-to-dependencies
  (testing "All services should be sorted according to dependencies"
    (let [
          pipeline-service 
          (create-service 
            {:name :pipeline-service 
             :dependencies [:event-service :dao-service :http-service] 
             :configuration {}})
          configuration-service 
          (create-service 
            {:name :configuration-service 
             :dependencies [] 
             :configuration {}})
          http-service 
          (create-service 
            {:name :http-service 
             :dependencies [:configuration-service] 
             :configuration {}})
          dao-service 
          (create-service 
            {:name :dao-service 
             :dependencies [:configuration-service :event-service :db-service] 
             :configuration {}})
          event-service 
          (create-service 
            {:name :event-service 
             :dependencies [] 
             :configuration {}})
          db-service 
          (create-service
            {:name :db-service 
             :dependencies [] 
             :configuration {}})
          declared-services
            [pipeline-service
            configuration-service
            http-service
            dao-service
            event-service
            db-service]
          sorted-services (sm/sort-services declared-services)]
      (is (= [configuration-service http-service event-service db-service dao-service pipeline-service] sorted-services))))
  (testing "Exception should be thrown if there are unsatisfied dependencies"
    (let [
          pipeline-service 
          (create-service 
            {:name :pipeline-service 
             :dependencies [:event-service :dao-service :http-service] 
             :configuration {}})
          configuration-service 
          (create-service 
            {:name :configuration-service 
             :dependencies [] 
             :configuration {}})
          declared-services
            [pipeline-service
            configuration-service]]
      (is (thrown? Throwable (sm/sort-services declared-services)))))
  (testing "Exception should be thrown if there is a cycle in dependencies"
    (let [
          pipeline-service 
          (create-service 
            {:name :pipeline-service 
             :dependencies [:event-service :dao-service :http-service] 
             :configuration {}})
          configuration-service 
          (create-service 
            {:name :configuration-service 
             :dependencies [] 
             :configuration {}})
          http-service 
          (create-service 
            {:name :http-service 
             :dependencies [:configuration-service] 
             :configuration {}})
          dao-service 
          (create-service 
            {:name :dao-service 
             :dependencies [:configuration-service :db-service] 
             :configuration {}})
          event-service 
          (create-service 
            {:name :event-service 
             :dependencies [:dao-service] 
             :configuration {}})
          db-service 
          (create-service 
            {:name :db-service 
             :dependencies [:event-service] 
             :configuration {}})
          declared-services
            [pipeline-service
            configuration-service
            http-service
            dao-service
            event-service
            db-service]]
      (is (thrown? Throwable (sm/sort-services declared-services))))))

