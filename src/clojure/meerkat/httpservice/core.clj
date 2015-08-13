(ns meerkat.httpservice.core)

(defprotocol HttpService
  (set-router [this router] "set router"))