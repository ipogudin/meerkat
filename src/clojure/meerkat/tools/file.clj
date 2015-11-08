(ns meerkat.tools.file
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:import [java.nio.file Path Paths Files FileSystems StandardWatchEventKinds
                          WatchService WatchKey WatchEvent FileVisitResult OpenOption LinkOption]
           [java.nio.file.attribute FileAttribute]
           [java.nio.file.attribute BasicFileAttributes]
           [java.io IOException]))

(def ENTRY_CREATE StandardWatchEventKinds/ENTRY_CREATE)
(def ENTRY_DELETE StandardWatchEventKinds/ENTRY_DELETE)
(def ENTRY_MODIFY StandardWatchEventKinds/ENTRY_MODIFY)
(def EMPTY_STRING_ARRAY (make-array String 0))
(def EMPTY_OPEN_OPTION_ARRAY (make-array OpenOption 0))
(def EMPTY_LINK_OPTION_ARRAY (make-array LinkOption 0))
(def EMPTY_FILE_ATTRIBUTES_ARRAY (make-array FileAttribute 0))

(gen-class
  :name meerkat.tools.file.walker
  :extends java.nio.file.SimpleFileVisitor
  :state "state"
  :init "init"
  :constructors {[] []}
  :prefix "visitor-")

(defn visitor-init []
  [[] {:files (atom []) :dirs (atom [])}])

(defn visitor-preVisitDirectory
  [this ^Path dir ^BasicFileAttributes attrs]
  (FileVisitResult/CONTINUE))

(defn visitor-visitFile
  [this ^Path file ^BasicFileAttributes attrs]
  (swap! (-> this (.state) (:files)) conj file)
  (FileVisitResult/CONTINUE))

(defn visitor-visitFileFailed
  [this ^Path file ^IOException e]
  (FileVisitResult/CONTINUE))

(defn visitor-postVisitDirectory
  [this ^Path dir ^IOException e]
  (swap! (-> this (.state) (:dirs)) conj dir)
  (FileVisitResult/CONTINUE))

(defn path
  ([p] (Paths/get p EMPTY_STRING_ARRAY))
  ([p & more] (Paths/get p (into-array more))))

(defn delete
  [p]
  (Files/delete p))

(defn read-bytes
  [p]
  (Files/readAllBytes p))

(defn create-file
  [p]
  (Files/createFile p EMPTY_FILE_ATTRIBUTES_ARRAY))

(defn create-dir
  [p]
  (Files/createDirectories p EMPTY_FILE_ATTRIBUTES_ARRAY))

(defn write-bytes
  [p bytes]
  (Files/write p bytes EMPTY_OPEN_OPTION_ARRAY))

(defn file?
  [p]
  (Files/isRegularFile p (into-array [LinkOption/NOFOLLOW_LINKS])))

(defn dir?
  [p]
  (Files/isDirectory p (into-array [LinkOption/NOFOLLOW_LINKS])))

(defn walk-file-tree
  "Returns a vector of Path objects corresponding with files from p parameter."
  [p]
  (let [w (meerkat.tools.file.walker.)]
    (Files/walkFileTree p w)
    (let [s (.state w)]
      {:files (deref (:files s)) :dirs (deref (:dirs s))})))

(defn rdelete
  [p]
  (let [r (walk-file-tree p)]
    (mapv delete (:files r))
    (mapv delete (:dirs r)))
  true)

(defn- register-watcher [keys watch-service p]
  (doseq [d (conj (:dirs (walk-file-tree p)) p)]
    (let [key
          (.register
            d
            watch-service
            (into-array [ENTRY_CREATE ENTRY_MODIFY ENTRY_DELETE]))]
      (swap! keys assoc key d))))

(defn- create-watcher [keys watch-service callback ]
  #(try+
    (while true
      (let [key (.take watch-service)]
        (doseq [e (.pollEvents key)]
          (let [kind (case (.. e kind name)
                       "ENTRY_CREATE" :create
                       "ENTRY_DELETE" :delete
                       "ENTRY_MODIFY" :modify)
                p (Paths/get (str (get @keys key)) (into-array [(str (.context e))]))]
            (if (and (= :create kind) (dir? p))
              (register-watcher keys watch-service p))
            (callback p kind)))
        (if (not (.reset key))
          (swap! keys dissoc key))))
    (catch InterruptedException e)))

(defn watch-file-tree
  "Watches file tree. Invokes callback with Path and :create|:modify|:delete as arguments."
  [p callback]
  (let [watch-service (.. FileSystems getDefault newWatchService)
        keys (atom {})
        t (Thread. (create-watcher keys watch-service callback))]
    (register-watcher keys watch-service p)
    (.setName t "file-tree-watcher")
    (.start t)
    {:watch-service watch-service :thread t}))

(defn stop-file-tree-watcher
  [{:keys [watch-service ^Thread thread]}]
  (.interrupt thread)
  (.close watch-service))
