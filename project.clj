(defproject meerkat "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [io.netty/netty-codec-http "4.1.0.Beta4"]
                 [io.netty/netty-handler "4.1.0.Beta4"]
                 [org.apache.httpcomponents/httpclient "4.5"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.javassist/javassist "3.19.0-GA"]])
