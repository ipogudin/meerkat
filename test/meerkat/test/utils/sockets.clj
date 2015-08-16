(ns meerkat.test.utils.sockets
  (:import [java.net Socket InetSocketAddress SocketTimeoutException]
           [java.io BufferedOutputStream BufferedWriter OutputStreamWriter])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(defn socket
  [host port timeout]
  (doto 
    (Socket.)
    (.connect (InetSocketAddress. host port) timeout)))

(defn wait-for-closing
  "waits during timeout until socket is not closed from the other side."
  [socket timeout]
  (.setSoTimeout socket timeout)
  (try+
    (while (not= (-> socket (.getInputStream) (.read)) -1))
    true
    (catch SocketTimeoutException _
      false)))

(defn write-and-flush
  [socket s]
  (doto 
    (-> socket (.getOutputStream) (OutputStreamWriter.) (BufferedWriter.))
    (.write s)
    (.flush)))