(ns meerkat.httpservice.default-handler)

(defn respond [context response-body content-type]
  ((:write-and-flush context)
     (-> context
       (assoc-in [:response :body] response-body)
       (assoc-in [:response :headers :content-type] content-type)
       (assoc-in [:response :headers :content-length] (count response-body))
       (assoc-in [:response :status] 200))))

(defn default-handler [context]
  (case (get-in context [:request :method])
    :GET (let [response-body (.getBytes "Hello world, from meerkat!" "UTF-8")]
           (respond context response-body "text/plain; charset=UTF-8"))
    :POST (let [response-body (get-in context [:request :body])]
            (respond context response-body "text/plain; charset=UTF-8"))
    (let [response-body (.getBytes "Method not found" "UTF-8")]
      (respond context response-body "text/plain; charset=UTF-8")))
  ((:complete context)))