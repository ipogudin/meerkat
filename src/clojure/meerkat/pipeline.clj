(ns meerkat.pipeline
  ;  (:require )
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:import [java.util.concurrent CompletableFuture Executor]
          [meerkat.java.pipeline Step]))

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

(defmacro pipeline
  "This macro creates pipeline and
  returns a function which run context execution in just created pipeline.
  It assumes that steps describe whole pipeline."
  [& steps]
  `(fn [context#]
     (let [p# (initiate-pipeline)]
       (-> p#
           ~@(map
              (fn [s]
                (let [{:keys [f async executor pass-if-error] :or {async false pass-if-error true}} (apply hash-map s)]
                  (if async
                    (if (nil? executor)
                      `(add-async-step (step ~f ~pass-if-error))
                      `(add-async-step (step ~f ~pass-if-error) ~executor))
                    `(add-step (step ~f ~pass-if-error)))))
              steps))
       (run-pipeline p# context#))))
