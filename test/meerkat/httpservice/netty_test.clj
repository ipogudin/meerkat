(ns meerkat.httpservice.netty-test
  (:import [java.util Arrays])
  (:require [clojure.test :refer :all]
            [meerkat.httpservice.netty :as http-service]
            [meerkat.test.utils.httpclient :as test-http-client]))

(def ^:dynamic http-service-instance)
(def ^:dynamic http-client-instance)

(defn http-service-fixture
  [f]
  (binding 
    [http-service-instance (http-service/start {})
     http-client-instance (test-http-client/start)]
    (f)
    (http-service/stop http-service-instance)
    (test-http-client/stop http-client-instance)))

(use-fixtures :once http-service-fixture)

(deftest get-method-test
  (testing "Mock get request to service"
    (is (= 200 (:code (test-http-client/get http-client-instance "http://localhost:8080/"))))))

(deftest post-method-test
  (testing "Mock post request to service"
    (let [
          body (.getBytes "body-body-body")
          response (test-http-client/post http-client-instance "http://localhost:8080/" body "text/plain; charset=UTF-8")]
    (is (= 200 (:code response)))
    (is (= (seq body) (seq (:body response)))))))
