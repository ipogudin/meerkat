(ns meerkat.httpservice.core)

(defprotocol HttpService
  (set-router [this router] "sets router"))

(defprotocol RequestHandler
  (handle [this context] "handles requests"))