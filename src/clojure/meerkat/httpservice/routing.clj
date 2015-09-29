(ns meerkat.httpservice.routing
  (:require [meerkat.common :as common]))

(set! *warn-on-reflection* true)

(defrecord RouteNode [type path following parameters handler matched-value])

(defn analyze-node-type
  [path]
  (cond
    (keyword? path) :parameter
    (instance? String path) :route-part))

(defn create-route-node
  ([path] (RouteNode. (analyze-node-type path) path [] nil nil nil))
  ([path following] (RouteNode. (analyze-node-type path) path following nil nil nil)))

(defn create-terminator 
  [values handler]
  (RouteNode. :terminator :terminator [] (filter keyword? values) handler nil))

(defn parse-route
  [route handler]
  (let
    [parsed-values
    (loop [[c & others] (seq route) current-element "" type :route-part result []]
      (if (nil? c)
        (cond
          (= type :route-part) (conj result current-element)
          (= type :parameter) (conj result (keyword current-element)))
        (cond
          (= c \:) (recur others "" :parameter (conj result current-element))
          (and (= type :parameter) (= c \/)) (recur others (str c) :route-part (conj result (keyword current-element)))
          :else (recur others (str current-element c) type result))))]
    (reduce 
      (fn 
        [previous path]
        (let [root (create-route-node path)]
          (assoc root :following [previous])))
      (create-terminator parsed-values handler)
      (vec (reverse parsed-values)))))

(defn path-overlap
  [s1 s2]
  (loop [[c1 & seq1] (seq s1) [c2 & seq2] (seq s2) i 0]
    (cond
      (or (nil? c1) (nil? c2)) i
      (not= c1 c2) i
      :else (recur seq1 seq2 (inc i)))))

(defn terminator?
  [node]
  (= :terminator (:type node)))

(defn not-terminator?
  [node]
  (not (terminator? node)))

(defn parameter?
  [node]
  (= :parameter (:type node)))

(defn route-part?
  [node]
  (= :route-part (:type node)))

(defn matched?
  [node]
  (some? (:matched-value node)))

(defn identical-parameters?
  "Do both nodes have identical wildcards."
  [node1 node2]
  (and (not-terminator? node1) (parameter? node1) (parameter? node2)))

(defn mergeable?
  "Can node1 be merged into node2."
  [node1 node2]
  (or
    (identical-parameters? node1 node2)
    (let [node1-path (:path node1)
          node2-path (:path node2)]
      (and
        (instance? String node1-path)
        (instance? String node2-path)
        (.startsWith ^String node1-path ^String node2-path)))))

(declare merge-node)

(defn add-node
  "add a node into vector or merge it with an appropriate node from a vector"
  [nodes node]
  (let [node-to-merge (first (filter (partial mergeable? node) nodes))]
    (if (nil? node-to-merge)
      (conj nodes node)
      (conj (filterv #(not= node-to-merge %) nodes) (merge-node node-to-merge node)))))

(defn merge-node
  "This function deeply merges a node passed as the second argument into a root passed as a firts argument."
  [root node]
  (let [root-path (:path root)
        node-path (:path node)
        route-parts (and (route-part? root) (route-part? node))
        i (if route-parts (path-overlap root-path node-path) -1)
        identical-nodes (or (identical-parameters? root node) (and (> i 0) (= i (count root-path)) (= i (count node-path))))
        new-root-path (if identical-nodes
                        root-path
                        (subs root-path 0 i))
        new-root-tail (if (> i 0) (subs root-path i (count root-path)) root-path)
        new-node-tail (if (> i 0) (subs node-path i (count node-path)) node-path)]
        (create-route-node
          new-root-path
          ; creation of a new vector of following route paths
          (cond
            identical-nodes
            (loop [[new-node & other-new-nodes] (:following node) result-nodes (:following root)]
              (if (nil? new-node)
                result-nodes
                (add-node result-nodes new-node)))
            (and (> i 0) (= i (count root-path)) (< i (count node-path)))
            (add-node (:following root) (create-route-node new-node-tail (:following node)))
            (and (> i 0) (< i (count root-path)))
            [(create-route-node new-root-tail (:following root))
             (create-route-node new-node-tail (:following node))]))))

(defrecord CommonRoutingConfiguration [routes])

(defrecord MethodRoutingConfiguration [GET HEAD POST PUT DELETE TRACE CONNECT])

(defn build-default-configuration []
  (CommonRoutingConfiguration. (MethodRoutingConfiguration. nil nil nil nil nil nil nil)))

(defn register-handler 
  ([method route handler] (register-handler (build-default-configuration) method route handler))
  ([configuration method route handler]
    (update-in
      configuration 
      [:routes method]
      (fn [root]
        (let [parsed-route (parse-route route handler)]
          (if (nil? root)
            parsed-route
            (merge-node root parsed-route)))))))

(defn register-default-handler 
  ([handler] (register-default-handler (build-default-configuration) handler))
  ([configuration handler] (assoc configuration :default-handler handler)))

(defmulti match-route (fn [uri route-node] (:type route-node)))

(defmethod match-route :terminator [uri route-node]
  route-node)

(defn match-parameter [node1 node2 ^String uri]
  (if (terminator? node2)
    uri
    (let [^String s2 (:path node2)
          n2 (count s2)
          n (count uri)]
      (loop [i2 0 i 0]
        (if (and (< i n) (< i2 n2))
          (let [c (.charAt uri i)
                c2 (.charAt s2 i2)]
            (if (= c c2)
              (if (= (inc i2) n2)
                (subs uri 0 (- i i2))
                (recur (inc i2) (inc i)))
              (recur 0 (inc i)))))))))

(defmethod match-route :parameter [uri route-node]
  (loop [[r & others] (:following route-node)]
    (if (nil? r)
      route-node
      (let [matched-path (match-parameter route-node r uri)]
        (if (some? matched-path)
          (assoc route-node :matched-value matched-path)
          (recur others))))))

(defmethod match-route :route-part [^String uri route-node]
  (let [^String path (:path route-node)]
    (if (.startsWith uri path)
      (assoc route-node :matched-value path)
      route-node)))

(defn process-path
  [path matched-node]
  (subs path (count (:matched-value matched-node))))

(defn find-matched-node
  "This function matches a path to route nodes.
  If there is proper route returns RouteNode else nil."
  [path [node & nodes]]
  (if (some? node)
    (let [n (match-route path node)]
      (if (matched? n) n (recur path nodes)))))

(defn find-handler
  "This function matches a uri against a root (a tree from RouteNode) to find a request handler."
  [root path]
  (loop [path (common/decode-url path) nodes [root] parameter-values (transient [])]
    (let [first-node (first nodes)]
      (if (and (terminator? first-node) (empty? path))
        (fn [context]
          ((:handler first-node) ; handler function
            (update-in
              context
              [:request :parameters]
              #(merge % (zipmap (:parameters first-node) (persistent! parameter-values)))))) ; updating context with uri parameters
        (if (and (seq path) first-node)
          (let [matched-node (find-matched-node path nodes)]
            (if-not (nil? matched-node)
              (recur
                (process-path path matched-node)
                (:following matched-node)
                (if (parameter? matched-node) (conj! parameter-values (:matched-value matched-node)) parameter-values)))))))))

(defn build-router [configuration]
  (fn [context]:following
    ((or
       (let [
             {method :method ^String uri :uri} (:request context)
             i (.indexOf uri (int \?))
             path (if (> i 0) (subs uri 0 i) uri)]
         (find-handler (get-in configuration [:routes method]) path))
       (:default-handler configuration))
      context)))

(defn print-route
  [root]
  (loop [nodes [root]]
    (if (not (empty? nodes))
      (do
        (println (mapv :path nodes))
        (recur (into [] (apply concat (mapv :following nodes))))))))