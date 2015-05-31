(ns meerkat.httpservice.default-handler)

(defn default-handler [context]
  (case (get-in context [:request :method])
    :GET (
           (:write-and-flush context)
           (let [response-body (.getBytes "Hello world, from meerkat!" "UTF-8")]
             (-> context
               (assoc-in [:response :body] response-body)
               (assoc-in [:response :headers :content-type] "text/plain")
               (assoc-in [:response :headers :content-length] (count response-body))
               (assoc-in [:response :status] 200))))
    :POST (
            (:write-and-flush context)
            (let [response-body (get-in context [:request :body])]
              (-> context
               (assoc-in [:response :body] response-body)
               (assoc-in [:response :headers :content-type] "text/plain")
               (assoc-in [:response :headers :content-length] (count response-body))
               (assoc-in [:response :status] 200))))
    (
      (:write-and-flush context) 
      (let [response-body (.getBytes "Method not found" "UTF-8")]
        (-> context
               (assoc-in [:response :body] response-body)
               (assoc-in [:response :headers :content-type] "text/plain")
               (assoc-in [:response :headers :content-length] (count response-body))
               (assoc-in [:response :status] 200)))))
  ((:complete context)))