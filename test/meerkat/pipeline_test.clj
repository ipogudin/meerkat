(ns meerkat.pipeline-test
  (:require [clojure.test :refer :all]
            [meerkat.test.tools.common :as test-common]
            [meerkat.pipeline :as p])
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:import [java.util.concurrent Executors]))

(defn fixture
  [f]
  (test-common/clear-recorder)
  (f))

(use-fixtures :each fixture)

(defn step1
  [context complete]
  (complete (assoc context :s1 :passed)))

(defn step2
  [context complete]
  (complete (assoc context :s2 :passed)))

(defn step-with-exception
  [context complete]
  (throw+ {:type :something-bad}))

(defn step-with-raw-exception
  [context complete]
  (throw (RuntimeException. "Something bad")))

(defn final-step
  [context complete]
  (test-common/record context))

(deftest pipeline
  (testing "synchronous steps"
    (let [pipeline (p/pipeline
                     [:f step1]
                     [:f final-step :pass-if-error false])
          pipeline-run (pipeline {})
          recorded-context (test-common/get-recorded)]
      (is pipeline-run)
      (is (= :passed (:s1 recorded-context)))))
  (testing "synchronous steps with an exception"
    (let [pipeline (p/pipeline
                     [:f step1]
                     [:f step-with-exception]
                     [:f step2]
                     [:f final-step :pass-if-error false])
          pipeline-run (pipeline {})
          recorded-context (test-common/get-recorded)]
      (is pipeline-run)
      (is (= :passed (:s1 recorded-context)))
      (is (some? (:error recorded-context)))
      (is (= :something-bad (get-in recorded-context [:error :type])))
      (is (nil? (:s2 recorded-context)))))
  (testing "synchronous steps with a raw exception"
    (let [pipeline (p/pipeline
                     [:f step1]
                     [:f step-with-raw-exception]
                     [:f step2]
                     [:f final-step :pass-if-error false])
          pipeline-run (pipeline {})
          recorded-context (test-common/get-recorded)]
      (is pipeline-run)
      (is (= :passed (:s1 recorded-context)))
      (is (some? (:error recorded-context)))
      (is (= :raw (get-in recorded-context [:error :type])))
      (is (nil? (:s2 recorded-context)))))
  (testing "asynchronous steps"
    (let [pipeline (p/pipeline
                     [:f step1 :async true]
                     [:f final-step :async true :pass-if-error false])
          pipeline-run (pipeline {})
          recorded-context (test-common/get-recorded)]
      (is pipeline-run)
      (is (= :passed (:s1 recorded-context)))))
  (testing "asynchronous steps with custom executor"
    (let [executor (Executors/newFixedThreadPool 5)
          pipeline (p/pipeline
                     [:f step1 :async true :executor executor]
                     [:f final-step :async true :executor executor :pass-if-error false])
          pipeline-run (pipeline {})
          recorded-context (test-common/get-recorded)]
      (is pipeline-run)
      (is (= :passed (:s1 recorded-context)))))
  (testing "asynchronous steps"
    (let [pipeline (p/pipeline
                     [:f step1 :async true]
                     [:f final-step :async true :pass-if-error false])
          pipeline-run (pipeline {})
          recorded-context (test-common/get-recorded)]
      (is pipeline-run)
      (is (= :passed (:s1 recorded-context)))))
  (testing "serial fork"
    (let [pipeline (p/pipeline
                     [:fork :serial :steps [[:f step1] [:f step2]] :reducer merge]
                     [:f final-step :pass-if-error false])
          pipeline-run (pipeline {})
          recorded-context (test-common/get-recorded)]
      (is pipeline-run)
      (is (= :passed (:s1 recorded-context)))
      (is (= :passed (:s2 recorded-context)))))
  (testing "parallel fork"
    (let [pipeline (p/pipeline
                     [:fork :parallel :steps [[:f step1] [:f step2]] :reducer merge]
                     [:f final-step :pass-if-error false])
          pipeline-run (pipeline {})
          recorded-context (test-common/get-recorded)]
      (is pipeline-run)
      (is (= :passed (:s1 recorded-context)))
      (is (= :passed (:s2 recorded-context)))))
  (testing "parallel fork with executor"
    (let [executor (Executors/newFixedThreadPool 5)
          pipeline (p/pipeline
                     [:fork :parallel :steps [[:f step1] [:f step2]] :executor executor :reducer merge]
                     [:f final-step :pass-if-error false])
          pipeline-run (pipeline {})
          recorded-context (test-common/get-recorded)]
      (is pipeline-run)
      (is (= :passed (:s1 recorded-context)))
      (is (= :passed (:s2 recorded-context))))))
