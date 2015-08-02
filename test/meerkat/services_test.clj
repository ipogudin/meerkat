(ns meerkat.services_test
  (:require [clojure.test :refer :all]
            [meerkat.services :as services])
  (:import [meerkat.services Service]))

(defrecord TestService [name dependencies configuration]
  meerkat.services.Service
  (configure [this new-configuration] (swap! configuration (fn [_] new-configuration)))
  (start [this] "start service" (println "start"))
  (stop [this] "stop service" (println "stop")))

(defn- create-service [{name :name dependencies :dependencies configuration :configuration}] 
  (TestService. name dependencies (atom configuration)))

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
          sorted-services (services/sort-services declared-services)]
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
      (is (thrown? Throwable (services/sort-services declared-services)))))
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
      (is (thrown? Throwable (services/sort-services declared-services))))))

(defrecord TestService1 [name dependencies configuration started]
  meerkat.services.Service
  (configure [this new-configuration] (swap! configuration (fn [_] new-configuration)))
  (start [this] "start service" (reset! started true))
  (stop [this] "stop service" (reset! started false)))

(defrecord TestService2 [name dependencies configuration started test-service1]
  meerkat.services.Service
  (configure [this new-configuration] (swap! configuration (fn [_] new-configuration)))
  (start [this] "start service" (reset! started true))
  (stop [this] "stop service" (reset! started false)))

(defrecord TestService3 [name dependencies configuration started test-service1 test-service2]
  meerkat.services.Service
  (configure [this new-configuration] (swap! configuration (fn [_] new-configuration)))
  (start [this] "start service" (reset! started true))
  (stop [this] "stop service" (reset! started false)))

(deftest injecting-dependencies
  (testing "All declared dependencies should be injected"
    (let [
          test-service1 (TestService1. :test-service1 [] (atom {}) (atom false))
          test-service2 (TestService1. :test-service2 [:test-service1] (atom {}) (atom false))
          test-service3 (TestService1. :test-service3 [:test-service1 :test-service2] (atom {}) (atom false))
          services-with-dependencies (services/inject-dependencies (services/sort-services [test-service1 test-service2 test-service3]))
          test-service2-with-dependencies (nth services-with-dependencies 1)
          test-service3-with-dependencies (nth services-with-dependencies 2)]
      (is (= :test-service1 (-> test-service2-with-dependencies :test-service1 :name)))
      (is (= :test-service1 (-> test-service3-with-dependencies :test-service1 :name)))
      (is (= :test-service2 (-> test-service3-with-dependencies :test-service2 :name)))
      (is (= :test-service1 (-> test-service3-with-dependencies :test-service2 :test-service1 :name))))))

(deftest service-lifecycle
  (testing "Starting services"
    (let [
          test-service1 (TestService1. :test-service1 [] (atom {}) (atom false))
          test-service2 (TestService1. :test-service2 [] (atom {}) (atom false))
          test-service3 (TestService1. :test-service3 [] (atom {}) (atom false))
          services (services/start-all [test-service1 test-service2 test-service3])
          ]
      (is (deref (:started test-service1)))
      (is (deref (:started test-service2)))
      (is (deref (:started test-service3)))))
  (testing "Stopping services"
    (let [
          test-service1 (TestService1. :test-service1 [] (atom {}) (atom true))
          test-service2 (TestService1. :test-service2 [] (atom {}) (atom true))
          test-service3 (TestService1. :test-service3 [] (atom {}) (atom true))
          services (services/stop-all [test-service1 test-service2 test-service3])
          ]
      (is (not (deref (:started test-service1))))
      (is (not (deref (:started test-service2))))
      (is (not (deref (:started test-service3))))))
  (testing "Configuring services"
    (let [
          test-service1 (TestService1. :test-service1 [] (atom {}) (atom true))
          test-service2 (TestService1. :test-service2 [] (atom {}) (atom true))
          test-service3 (TestService1. :test-service3 [] (atom {}) (atom true))
          services (services/configure-all 
                     [test-service1 test-service2 test-service3] 
                     {:test-service1 {:value 1} 
                      :test-service2 {:value 2}})
          ]
      (is (= 1 (:value (deref (:configuration test-service1)))))
      (is (= 2 (:value (deref (:configuration test-service2))))))))