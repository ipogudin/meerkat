(ns meerkat.pipeline
  ;  (:require )
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:import [java.util.concurrent CompletableFuture Executor]
          [meerkat.java.pipeline Step SerialFork ParallelFork]))

(defmacro error?
  "Does this context contain an error?"
  [context]
  `(some? (:error ~context)))

(defn set-error
  "Set an error in context"
  [context error throwable]
  (let [e (if (map? error) error {:type :raw})]
    (assoc context :error (assoc e :throwable throwable))))

(defn- wrap-step-function
  [f pass-if-error]
  (fn [context complete]
    (if (and pass-if-error (error? context))
      (complete context) ; pass there is an error in context
      (try+
        (f context complete)
        (catch Object e
          (complete (set-error context e (:throwable &throw-context))))))))

(defn step
  [f pass-if-error]
  (Step. (wrap-step-function f pass-if-error)))

(defn serial-fork
  [steps reducer]
  (SerialFork. steps reducer))

(defn parallel-fork
  ([steps reducer] (ParallelFork. steps reducer))
  ([steps reducer executor] (ParallelFork. steps reducer executor)))

(defn initiate-pipeline
  []
  (CompletableFuture.))

(defn add-step
  [^CompletableFuture pipeline ^Step step]
  (.thenCompose pipeline step))

(defn add-async-step
  ([^CompletableFuture pipeline ^Step step]
   (.thenComposeAsync pipeline step))
  ([^CompletableFuture pipeline ^Step step ^Executor executor]
    (.thenComposeAsync pipeline step executor)))

(defn run-pipeline
  [^CompletableFuture pipeline ^Step context]
  (.complete pipeline context))

(defn- encode-step
  [s]
  (let [sd (apply hash-map s)
        {:keys [f pass-if-error] :or {pass-if-error true}} sd]
    `(step ~f ~pass-if-error)))

(defn- encode-joint
  [s]
  (let [sd (apply hash-map s)
        {:keys [fork steps reducer async executor] :or {async false}} sd]
    (cond
      (= fork :serial)
      (if async
        (if (nil? executor)
          `(add-async-step (serial-fork ~(mapv encode-step steps) ~reducer))
          `(add-async-step (serial-fork ~(mapv encode-step steps) ~reducer) ~executor))
        `(add-step (serial-fork ~(mapv encode-step steps) ~reducer)))
      (= fork :parallel)
      (if async
        (if (nil? executor)
          `(add-async-step (parallel-fork ~(mapv encode-step steps) ~reducer))
          `(add-async-step (parallel-fork ~(mapv encode-step steps) ~reducer ~executor) ~executor))
        (if (nil? executor)
          `(add-step (parallel-fork ~(mapv encode-step steps) ~reducer))
          `(add-step (parallel-fork ~(mapv encode-step steps) ~reducer ~executor))))
      :else
      (let [{:keys [async executor] :or {async false}} sd]
        (if async
          (if (nil? executor)
            `(add-async-step ~(encode-step s))
            `(add-async-step ~(encode-step s) ~executor))
          `(add-step ~(encode-step s)))))))

(defmacro pipeline
  "This macro creates pipeline and
  returns a function which run context execution in just created pipeline.
  It assumes that steps describe whole pipeline."
  [& steps]
  `(fn [context#]
     (let [p# (initiate-pipeline)]
       (-> p#
           ~@(map encode-joint steps))
       (run-pipeline p# context#))))
