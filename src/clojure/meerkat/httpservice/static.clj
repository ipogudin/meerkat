(ns meerkat.httpservice.static
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure.tools.logging :as log]
            [clojure.core.memoize :as memo]
            [meerkat.services :as s]
            [meerkat.tools.file :as f]
            [meerkat.httpservice.default-handler :as handlers])
  (:import [meerkat.services Service]
           [meerkat.httpservice.core RequestHandler]
           [java.nio.file Path]))

(def ^:dynamic *default-configuration*
  {
   :url-prefix "/"
   :directory "/tmp/www"
   :cache-threshold 25})

(defn- add-resource
  [^String root-dir resources ^Path path resource-cache]
  (log/debug (format "Adding resource %s" (str path)))
  (let [files (if
                (f/dir? path)
                (:files (f/walk-file-tree path))
                [path])
        prefix-to-paths (mapv
                          (fn [p]
                            [(subs (str p) (count root-dir)) p])
                          files)]
    (if (seq prefix-to-paths)
      (do
        (apply
          swap!
          resources
          assoc
          (flatten prefix-to-paths))
        (memo/memo-clear! @resource-cache (map first prefix-to-paths))))))

(defn- change-resource
  [^String root-dir resources ^Path path resource-cache]
  )

(defn- delete-resource
  [^String root-dir resources ^Path path resource-cache]
  (let [sub-path (subs (str path) (count root-dir))
        keys-to-delete (filterv #(.startsWith % sub-path) (keys resources))]
    (swap! resources dissoc keys-to-delete)
    (memo/memo-clear! @resource-cache keys-to-delete)))

(defn- create-watch-handler
  [^String root-dir resources resource-cache]
  (fn [^Path path kind]
    (log/debug (format "File-tree has been changed %s %s" (str path) (str kind)))
    (case kind
      :create (add-resource root-dir resources path resource-cache)
      :modify #()
      :delete (delete-resource resources resources path resource-cache))))

(defn read-resource
  [resources relative-path]
  (let [p (get @resources relative-path)]
    (if p
      [(f/read-bytes p) "text/plain; charset=UTF-8"])))

(defrecord StaticService [name dependencies configuration watcher resources resource-cache]
  Service
  (start [this]
    (let [root-dir (-> this :configuration deref :directory)]
      (swap!
        watcher
        (fn [_]
          (f/watch-file-tree
            (f/path root-dir)
            (create-watch-handler root-dir resources resource-cache))))
      (add-resource root-dir resources (f/path root-dir) resource-cache))
    (log/info "static-service just started")
    this)
  (stop [_]
    (swap! watcher (fn [w] (f/stop-file-tree-watcher w) nil))
    (swap! resources (fn [_] {}))
    (log/info "static-service just stopped"))
  (configure [this new-configuration]
    (swap! (:configuration this) #(merge % new-configuration))
    (let [conf (-> this :configuration deref)]
      (vreset!
        (:resource-cache this)
        (memo/lru (partial read-resource (:resources this)) {} :lru/threshold (:cache-threshold conf))))
    this)
  RequestHandler
  (handle [this context]
    (let [url-prefix (-> this :configuration deref :url-prefix)
          relative-path (subs (get-in context [:request :uri]) (count url-prefix))]
      (apply
        handlers/respond
        context
        ((-> this :resource-cache deref) relative-path)))))

(defn create-static-service
  [name dependencies]
  (->StaticService
    name
    dependencies
    (atom *default-configuration*)
    (atom nil)
    (atom {})
    (volatile! nil)))