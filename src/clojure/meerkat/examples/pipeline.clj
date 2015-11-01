(ns meerkat.examples.pipeline
  (:gen-class)
  (:require
            [meerkat.examples.rest :as r]
            [meerkat.pipeline :as p]
            [meerkat.httpservice.default-handler :as handlers]
            [meerkat.httpservice.routing :as routing]))

(defn final-step [context complete]
  (let [response-body
        (.getBytes
          (str context)
          "UTF-8")]
    (handlers/respond context response-body "text/plain; charset=UTF-8"))
  ((:complete context)))

(defn s1 [context complete]
  (complete (assoc context :s1 :passed)))

(defn s2 [context complete]
  (complete (assoc context :s2 :passed)))

(def pp
  (p/pipeline
    [:fork :parallel :steps [[:f s1] [:f s2]] :reducer merge]
    [:f final-step :pass-if-error true]))

(def routes-configuration
  (->
    (routing/register-default-handler handlers/default-handler)
    (routing/register-handler :GET "/pipeline" pp)))

(defn -main [& args]
  (r/start-http-service (r/configure-routing routes-configuration)))