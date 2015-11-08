(ns meerkat.httpservice.static
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:require [meerkat.services :as s]
            [meerkat.tools.file :as f]
            [clojure.tools.logging :as log]
            [meerkat.httpservice.default-handler :as handlers])
  (:import [meerkat.services Service]
           [meerkat.httpservice.core RequestHandler]
           [java.nio.file Path]))

(def ^:dynamic *default-configuration*
  {
   :url-prefix "/"
   :directory "/tmp/www"})

(defn- add-resource
  [^String root-dir resources ^Path path]
  (log/debug (format "Adding resource %s" (str path)))
  (let [files (if (f/dir? path) (:files (f/walk-file-tree path)) [path])]
    (apply
      swap!
      resources
      assoc
      (flatten
        (mapv
          (fn [p]
            [(subs (str p) (count root-dir)) p])
          files)))))

(defn- delete-resource
  [^String root-dir resources ^Path path]
  (let [sub-path (subs (str path) (count root-dir))]
    (swap! resources dissoc sub-path)))

(defn- create-watch-handler
  [^String root-dir resources]
  (fn [^Path path kind]
    (log/debug (format "File-tree has been changed %s %s" (str path) (str kind)))
    (case kind
      :create (add-resource root-dir resources path)
      :modify #()
      :delete (delete-resource resources resources path))))

(defrecord StaticService [name dependencies configuration watcher resources]
  Service
  (start [this]
    (let [root-dir (:directory (deref (:configuration this)))]
      (swap!
        watcher
        (fn [_]
          (f/watch-file-tree
            (f/path root-dir)
            (create-watch-handler root-dir resources))))
      (add-resource root-dir resources (f/path root-dir)))
    (log/info "static-service just started")
    this)
  (stop [_]
    (swap! watcher (fn [w] (f/stop-file-tree-watcher w) nil))
    (swap! resources (fn [_] {}))
    (log/info "static-service just stopped"))
  (configure [_ new-configuration]
    (swap! configuration #(merge % new-configuration)))
  RequestHandler
  (handle [this context]
    (println @resources)
    (handlers/respond context (f/read-bytes (get @resources (get-in context [:request :parameters :resource]))) "text/plain; charset=UTF-8")))

(defn create-static-service
  [name dependencies]
  (->StaticService
    name
    dependencies
    (atom *default-configuration*)
    (atom nil)
    (atom {})))