(ns meerkat.test.utils.httpclient
  (:import [org.apache.http.entity ContentType]
           [org.apache.http.util EntityUtils]
           [org.apache.http.impl.client HttpClients]
           [org.apache.http.client.methods HttpGet]
           [org.apache.http.client.methods HttpPost]
           [org.apache.http.entity ByteArrayEntity]))

(defn extract-client
  [client]
  (:http-client (meta client)))

(defn- transform-headers
  [response]
  (map 
        (fn [h] {(-> h .getName .toLowerCase) (.getValue h)})
        (.getAllHeaders response)))

(defn- transform-response
  [response]
  {:code (-> response .getStatusLine .getStatusCode)
   :headers (transform-headers response)
   :body (-> response .getEntity org.apache.http.util.EntityUtils/toByteArray)})

(defn get
  [client url]
  (let [response (.execute
                  (extract-client client)
                  (org.apache.http.client.methods.HttpGet. url))]
      (transform-response response)))

(defn post
  [client url body content-type]
  (let [response (.execute
                  (extract-client client)
                  (doto
                    (org.apache.http.client.methods.HttpPost. url)
                    (.setEntity 
                      (org.apache.http.entity.ByteArrayEntity. 
                        body 
                        (org.apache.http.entity.ContentType/parse content-type)))))]
      (transform-response response)))

(defn start
  []
  (with-meta
    {}
    {:http-client (org.apache.http.impl.client.HttpClients/createDefault)}))

(defn stop
  [client]
  (-> (extract-client client) .close))