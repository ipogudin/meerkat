(ns meerkat.tools.file-test
  (:require [clojure.test :refer :all]
            [meerkat.test.tools.common :as test-common]
            [meerkat.tools.file :as f])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def TEST_DIR "/tmp/test")

(defn p [& paths]
  (apply f/path TEST_DIR paths))

(defn fixture
  [f]
  (try+
    (test-common/clear-recorder)
    (f)
    (finally
      (f/rdelete (p)))))

(use-fixtures :each fixture)

(deftest file-tools
  (testing "walking-file-tree"
    (f/create-dir (p "dir1"))
    (f/create-file (p "file1"))
    (f/create-file (p "file2"))
    (f/create-file (p "dir1" "file3"))
    (let [r (f/walk-file-tree (p))
          files (apply hash-set (map str (:files r)))
          dirs (apply hash-set (map str (:dirs r)))]
      (is (contains? files (str (p  "file1"))))
      (is (contains? files (str (p "file2"))))
      (is (contains? files (str (p "dir1" "file3"))))
      (is (contains? dirs (str (p))))
      (is (contains? dirs (str (p "dir1")))))))

(defmacro expect
  [p kind]
  `(= {:path (str (p ~@p)) :kind ~kind} (test-common/get-recorded)))

(deftest watching-file-tree
  (testing "Creation, modification and deletion should be watched"
    (f/create-dir (p))
    (let [w (f/watch-file-tree
              (p)
              (fn
                [path kind]
                (test-common/record {:path (str path) :kind kind})))]
      (try+
        (f/create-dir (p "dir1"))
        (is (= {:path (str (p "dir1")) :kind :create} (test-common/get-recorded)))
        (f/create-file (p "dir1" "file2"))
        (is (= {:path (str (p "dir1" "file2")) :kind :create} (test-common/get-recorded)))
        (f/create-file (p "file1"))
        (is (= {:path (str (p "file1")) :kind :create} (test-common/get-recorded)))
        (f/write-bytes (p "file1") (.getBytes "something"))
        (is (= {:path (str (p "file1")) :kind :modify} (test-common/get-recorded)))
        (is (= {:path (str (p "file1")) :kind :modify} (test-common/get-recorded)))
        (f/delete (p "dir1" "file2"))
        (is (= {:path (str (p "dir1" "file2")) :kind :delete} (test-common/get-recorded)))
        (finally (f/stop-file-tree-watcher w))))))
