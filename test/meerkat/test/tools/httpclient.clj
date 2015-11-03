(ns meerkat.test.tools.httpclient
  (:import [org.apache.http.entity ContentType]
           [org.apache.http.util EntityUtils]
           [org.apache.http.impl.client HttpClients]
           [org.apache.http.client.methods HttpGet]
           [org.apache.http.client.methods HttpPost]
           [org.apache.http.entity ByteArrayEntity]
           [org.apache.http.message BasicNameValuePair]
           [org.apache.http.client.entity UrlEncodedFormEntity]
           [org.apache.http.entity.mime MultipartEntityBuilder]))

(defn- extract-client
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

(defn- set-headers
  [^org.apache.http.client.methods.HttpUriRequest uriRequest headers]
  (doseq [entry headers] (.addHeader uriRequest (name (key entry)) (val entry)))
  uriRequest)

(defn get-available-port []
  (.getLocalPort 
    (doto 
      (java.net.ServerSocket. 0)
      (.close))))

(defn GET
  "Performs HTTP GET request to url with provided headers"
  [client url headers]
  (let [response (.execute
                  (extract-client client)
                  (doto
                    (org.apache.http.client.methods.HttpGet. url)
                    (set-headers headers)))]
      (transform-response response)))

(defn byte-array-body [body content-type]
  "Creates a body from byte array with content-type"
  (org.apache.http.entity.ByteArrayEntity. 
    body 
    (org.apache.http.entity.ContentType/parse content-type)))

(defn url-encoded-body [parameters]
  "Encodes map of parameters into url encoded body
   keys can be as string as keywords
   values should be strings"
  (org.apache.http.client.entity.UrlEncodedFormEntity.
    (map 
      (fn [[n v]] (org.apache.http.message.BasicNameValuePair. (name n) v))
      parameters)))

(defn- prepare-multipart-value [entity name {value :value content-type :content-type filename :filename}]
  (.addBinaryBody 
    entity 
    name 
    value 
    (org.apache.http.entity.ContentType/parse content-type)
    filename))

(defn multipart-body [parameters]
  (let [entity (org.apache.http.entity.mime.MultipartEntityBuilder/create)]
    (doseq [[n v] parameters] (prepare-multipart-value entity (name n) v))
    (.build entity)))


(defn POST
  [client url body headers]
  "Performs HTTP POST request to url with provided body and headers
   headers must at least contain :content-type"
  (let [response (.execute
                  (extract-client client)
                  (doto
                    (org.apache.http.client.methods.HttpPost. url)
                    (.setEntity body)
                    (set-headers headers)))]
      (transform-response response)))

(defn start
  []
  (with-meta
    {}
    {:http-client (org.apache.http.impl.client.HttpClients/createDefault)}))

(defn stop
  [client]
  (-> (extract-client client) .close))