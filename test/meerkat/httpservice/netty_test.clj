(ns meerkat.httpservice.netty-test
  (:import [java.util Arrays])
  (:require [clojure.test :refer :all]
            [meerkat.httpservice.netty :as http-service]
            [meerkat.test.utils.httpclient :as test-http-client]
            [meerkat.httpservice.default-handler :as handlers]
            [meerkat.httpservice.routing :as routing]))

(def ^:dynamic http-service-instance)
(def ^:dynamic http-client-instance)

(def port (test-http-client/get-available-port))

(defn http-service-fixture
  [f]
  (binding 
    [http-service-instance (http-service/start {:port port})
     http-client-instance (test-http-client/start)]
    ;configuring routing and setting default handler
    (http-service/set-router 
      http-service-instance
      (->
        (routing/register-default-handler handlers/default-handler)
        (routing/build-router)))
    (f)
    (http-service/stop http-service-instance)
    (test-http-client/stop http-client-instance)))

(use-fixtures :once http-service-fixture)

(deftest get-method-test
  (testing "Mock get request to service"
    (let [response (test-http-client/get http-client-instance (format "http://localhost:%d/" port) {})]
    (is (= 200 (:code response))))))

(deftest post-method-test
  (testing "Mock post request to service"
    (let [
          body (.getBytes "body-body-body")
          response (test-http-client/post 
                     http-client-instance 
                     (format "http://localhost:%d/" port) 
                     body 
                     {:content-type "text/plain; charset=UTF-8"})]
    (is (= 200 (:code response)))
    (is (= (seq body) (seq (:body response)))))))
