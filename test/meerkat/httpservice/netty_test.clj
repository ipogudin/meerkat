(ns meerkat.httpservice.netty-test
  (:import [java.util Arrays])
  (:require [clojure.test :refer :all]
            [meerkat.test.utils.common :as test-common]
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
        (routing/register-default-handler 
          (fn [context] 
            (test-common/record (:request context))
            (let [response-body (.getBytes "test response")]
            ((:write-and-flush context)
              (-> context
                (assoc-in [:response :body] response-body)
                (assoc-in [:response :headers :content-type] "text/plain; UTF-8")
                (assoc-in [:response :headers :content-length] (count response-body))
                (assoc-in [:response :status] 200)))
            ((:complete context)))))
        (routing/build-router)))
    (try
      (f)
      (finally
        (http-service/stop http-service-instance)
        (test-http-client/stop http-client-instance)))))

(use-fixtures :once http-service-fixture)

(deftest get-method-test
  (testing "GET request without url parameters to service"
    (let [response (test-http-client/GET 
                     http-client-instance 
                     (format "http://localhost:%d/" port) {})
          recorded-request (test-common/get-recorded)]
    (is (= 200 (:code response)))
    (is (= 0 (count (:parameters recorded-request))))))
  (testing "GET request with url encoded parameters to service"
    (let [response (test-http-client/GET 
                     http-client-instance 
                     (format "http://localhost:%d/?parameter1=val1&parameter2=val2%%26+" port) {})
          recorded-request (test-common/get-recorded)]
    (is (= 200 (:code response)))
    (is (= 2 (count (:parameters recorded-request))))
    (is (= ["val1"] (get-in recorded-request [:parameters :parameter1])))
    (is (= ["val2& "] (get-in recorded-request [:parameters :parameter2]))))))

(deftest post-method-test
  (testing "POST request to service with text body"
    (let [
          body (.getBytes "body-body-body")
          response (test-http-client/POST 
                     http-client-instance 
                     (format "http://localhost:%d/" port) 
                     (test-http-client/byte-array-body body "text/plain; charset=UTF-8")
                     {})
          recorded-request (test-common/get-recorded)]
      (is (= 200 (:code response)))
      (is (= (seq body) (seq (:body recorded-request))))
      (is (= 0 (count (:parameters recorded-request))))))
  (testing "POST request to service with url encoded"
    (let [
          parameters {:parameter1 "value1" :parameter2 "value2"}
          response (test-http-client/POST 
                     http-client-instance 
                     (format "http://localhost:%d/" port) 
                     (test-http-client/url-encoded-body parameters)
                     {})
          recorded-request (test-common/get-recorded)]
      (is (= 200 (:code response)))
      (is (= 2 (count (:parameters recorded-request))))
      (is (= ["value1"] (get-in recorded-request [:parameters :parameter1])))
      (is (= ["value2"] (get-in recorded-request [:parameters :parameter2])))))
  (testing "POST request to service with multipart body containing text/plain fields"
    (let [
          parameters {
                      :parameter1 {:value (.getBytes "value1") :content-type "text/plain; charset=UTF-8"} 
                      :parameter2 {:value (.getBytes "value2") :content-type "text/plain; charset=UTF-8"}}
          response (test-http-client/POST 
                     http-client-instance 
                     (format "http://localhost:%d/" port) 
                     (test-http-client/multipart-body parameters)
                     {})
          recorded-request (test-common/get-recorded)]
      (is (= 200 (:code response)))
      (is (= 2 (count (:parameters recorded-request))))
      (is (= ["value1"] (get-in recorded-request [:parameters :parameter1])))
      (is (= ["value2"] (get-in recorded-request [:parameters :parameter2]))))))
