(ns meerkat.httpservice.routing)

(defn build-default-configuration []
  {})

(defn register-handler 
  ([route handler] (register-handler (build-default-configuration) route handler))
  ([configuration route handler] (update configuration :static-routes (partial merge {route handler}))))

(defn register-default-handler 
  ([handler] (register-default-handler (build-default-configuration) handler))
  ([configuration handler] (register-handler configuration :default handler)))

(defn build-router [configuration]
  (fn [context] ((get-in configuration [:static-routes :default]) context)))