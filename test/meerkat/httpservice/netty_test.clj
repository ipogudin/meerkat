(ns meerkat.httpservice.netty-test
  (:require [clojure.test :refer :all]
            [meerkat.test.utils.common :as test-common]
            [meerkat.services :as services]
            [meerkat.httpservice.core :as http-service]
            [meerkat.httpservice.netty :as netty-http-service]
            [meerkat.test.utils.httpclient :as test-http-client]
            [meerkat.httpservice.default-handler :as handlers]
            [meerkat.httpservice.routing :as routing]))

(def ^:dynamic http-service-instance)
(def ^:dynamic http-client-instance)

(def port (test-http-client/get-available-port))

(defn http-service-fixture
  [f]
  (binding 
    [http-service-instance (services/start (netty-http-service/create-http-service "http-service" [] {:port port}))
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
        (services/stop http-service-instance)
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
                     (format "http://localhost:%d/?parameter1=value1&parameter2=value2%%26+" port) {})
          recorded-request (test-common/get-recorded)]
    (is (= 200 (:code response)))
    (is (= 2 (count (:parameters recorded-request))))
    (is (= ["value1"] (get-in recorded-request [:parameters :parameter1])))
    (is (= ["value2& "] (get-in recorded-request [:parameters :parameter2]))))))

(defn- POST [url-pattern body body-validator parameters-validator]
  (let [response (test-http-client/POST 
                   http-client-instance 
                   (format url-pattern port) 
                   body
                   {})
        recorded-request (test-common/get-recorded)]
    (is (= 200 (:code response)))
    (body-validator (:body recorded-request))
    (parameters-validator (:parameters recorded-request))))

(deftest post-method-test
  (testing "POST request to service with text body"
    (let [body (.getBytes "body-body-body")]
      (POST
        "http://localhost:%d/"
        (test-http-client/byte-array-body body "text/plain; charset=UTF-8")
        (fn [recorded-body] 
          (is (= (seq body) (seq recorded-body))))
        (fn [recorded-parameters]
          (is (= 0 (count recorded-parameters)))))))
  (testing "POST request to service with url encoded"
    (POST
      "http://localhost:%d/"
      (test-http-client/url-encoded-body 
        {:parameter1 "value1" :parameter2 "value2"})
      (fn [_] )
      (fn [recorded-parameters]
        (is (= 2 (count recorded-parameters)))
        (is (= ["value1"] (:parameter1 recorded-parameters)))
        (is (= ["value2"] (:parameter2 recorded-parameters))))))
  (testing "POST request to service with multipart body containing text/plain fields"
    (POST
      "http://localhost:%d/"
      (test-http-client/multipart-body 
        {
         :parameter1 {:value (.getBytes "value1") :content-type "text/plain; charset=UTF-8"} 
         :parameter2 {:value (.getBytes "value2") :content-type "text/plain; charset=UTF-8"}})
      (fn [_] )
      (fn [recorded-parameters]
        (is (= 2 (count recorded-parameters)))
        (is (= ["value1"] (:parameter1 recorded-parameters)))
        (is (= ["value2"] (:parameter2 recorded-parameters))))))
  (testing "POST request to service with multipart body containing text/plain fields with url encoded parameters"
    (POST
      "http://localhost:%d/?parameter3=value3&parameter4=value4%%26+"
      (test-http-client/multipart-body 
        {
         :parameter1 {:value (.getBytes "value1") :content-type "text/plain; charset=UTF-8"} 
         :parameter2 {:value (.getBytes "value2") :content-type "text/plain; charset=UTF-8"}})
      (fn [_] )
      (fn [recorded-parameters]
        (is (= 4 (count recorded-parameters)))
        (is (= ["value1"] (:parameter1 recorded-parameters)))
        (is (= ["value2"] (:parameter2 recorded-parameters)))
        (is (= ["value3"] (:parameter3 recorded-parameters)))
        (is (= ["value4& "] (:parameter4 recorded-parameters)))))))
