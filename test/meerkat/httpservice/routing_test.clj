(ns meerkat.httpservice.routing-test
  (:require [clojure.test :refer :all]
            [meerkat.test.utils.common :as test-common]
            [meerkat.httpservice.routing :as routing]))

(defn fixture
  [f]
  (test-common/clear-recorder)
  (f))

(use-fixtures :once fixture)

(deftest parsing-routes
  (testing "A route with a parameter should be parsed according to paths"
    (let 
      [route "/v1/item/:id/children"
       parsed-route (routing/parse-route route "handler-function")
       [node2 & _] (:following parsed-route)
       [node3 & _] (:following node2)
       [terminator & _] (:following node3)
       [id & parameters] (:parameters terminator)]
    (is (= "/v1/item/" (:path parsed-route)))
    (is (= :id (:path node2)))
    (is (= "/children" (:path node3)))
    (is (= :terminator (:path terminator)))
    (is (= :id id))
    (is (= "handler-function" (:handler terminator)))
    (is (nil? parameters))))
  (testing "A route with only slash should be parsed correctly"
    (let 
      [route "/"
       parsed-route (routing/parse-route route "handler-function")
       [terminator & _] (:following parsed-route)]
    (is (= "/" (:path parsed-route)))
    (is (= :terminator (:path terminator)))
    (is (= "handler-function" (:handler terminator))))))

(defn check-route-nodes
  [root key all-values]
  (loop [nodes [root]
         [values & other-values] all-values]
    (if (nil? (seq values))
      nil
      (do
        (is (= (sort (mapv str values)) (sort (mapv #(str (key %)) nodes))))
        (recur
          (vec 
            (apply 
              concat 
              (filter 
                #(not (nil? (seq %))) 
                (map :following nodes))))
          other-values)))))

(deftest merging-routes
  (testing "Two different routes must be merged with a slash root node."
    (let 
      [root (routing/parse-route "/v1/item/:id/children" nil)
       node (routing/parse-route "/item/:name/children" nil)
       merged-root (routing/merge-node root node)]
    (check-route-nodes merged-root :path [["/"] ["v1/item/" "item/"] [:id :name] ["/children" "/children"]])))
  (testing "Two different routes with identical beginning must be correctly merged."
    (let 
      [root (routing/parse-route "/v1/item/:id/children" nil)
       node (routing/parse-route "/v1/item/:id/details" nil)
       merged-root (routing/merge-node root node)]
    (check-route-nodes merged-root :path [["/v1/item/"] [:id] ["/children" "/details"]])))
  (testing "Four different routes must be correctly merged."
    (let
      [nodes [(routing/parse-route "/something/test1" nil)
              (routing/parse-route "/something/test1/test2" nil)
              (routing/parse-route "/test/:param1/list" nil)
              (routing/parse-route "/test/:param1/details/:param2/info" nil)]
       merged-root (reduce routing/merge-node nodes)]
      (check-route-nodes merged-root :path [["/"] ["something/test1" "test/"] [:param1 "/test2" :terminator] ["/list" "/details/" :terminator] [:param2 :terminator] ["/info"]]))))

(deftest routing
  (testing "Default handler must be invoked if there is no appropriate route."
     (let
       [routing-configuration (routing/register-default-handler test-common/record)
        router (routing/build-router routing-configuration)
        context {:request {:uri "/test/uri"}}]
       (router context)
       (let [recorded-context (test-common/get-recorded)]
         (is (= context recorded-context)))))
  (testing "Appropriate handler must be invoked for particular uri."
     (let
       [routing-configuration 
        (-> (routing/register-default-handler (fn [_]))
          (routing/register-handler "/test/uri" test-common/record)
          (routing/register-handler "/test/something" (fn [_])))
        router (routing/build-router routing-configuration)
        context {:request {:uri "/test/uri"}}]
       (router context)
       (let [recorded-context (test-common/get-recorded)]
         (is (= "/test/uri" (get-in recorded-context [:request :uri]))))))
  (testing "Appropriate handler must be invoked for particular uri with a parameter."
    (let
      [routing-configuration
       (-> (routing/register-default-handler (fn [_]))
           (routing/register-handler "/test/:param1/list" test-common/record)
           (routing/register-handler "/test/:param1/info" (fn [_])))
       router (routing/build-router routing-configuration)
       context {:request {:uri "/test/entity1/list"}}]
      (router context)
      (let [recorded-context (test-common/get-recorded)]
        (is (= "/test/entity1/list" (get-in recorded-context [:request :uri])))
        (is (= {:param1 "entity1"} (get-in recorded-context [:request :parameters]))))))
  (testing "Appropriate handler must be invoked for particular uri with two parameters."
    (let
      [routing-configuration
       (-> (routing/register-default-handler (fn [_]))
           (routing/register-handler "/something/test1" (fn [_]))
           (routing/register-handler "/something/test1/test2" (fn [_]))
           (routing/register-handler "/test/:param1/list" (fn [_]))
           (routing/register-handler "/test/:param1/details/:param2/info" test-common/record))
       router (routing/build-router routing-configuration)
       context {:request {:uri "/test/entity1/details/entity2/info"}}]
      (router context)
      (let [recorded-context (test-common/get-recorded)]
        (is (= "/test/entity1/details/entity2/info" (get-in recorded-context [:request :uri])))
        (is (= {:param1 "entity1" :param2 "entity2"} (get-in recorded-context [:request :parameters]))))))
  (testing "Appropriate handler must be invoked for particular uri with two parameters delimted by a slash."
    (let
      [routing-configuration
       (-> (routing/register-default-handler (fn [_]))
           (routing/register-handler "/test/:param1/list" (fn [_]))
           (routing/register-handler "/test/:param1/:param2/info" test-common/record))
       router (routing/build-router routing-configuration)
       context {:request {:uri "/test/entity1/entity2/info"}}]
      (router context)
      (let [recorded-context (test-common/get-recorded)]
        (is (= "/test/entity1/entity2/info" (get-in recorded-context [:request :uri])))
        (is (= {:param1 "entity1" :param2 "entity2"} (get-in recorded-context [:request :parameters])))))))