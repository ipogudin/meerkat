(defproject meerkat "0.0.1-SNAPSHOT"
  :description "Meerkat is a small http framework."
  :url "https://github.com/ipogudin/meerkat"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [slingshot "0.12.2"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [io.netty/netty-codec-http "4.1.0.Beta4"]
                 [io.netty/netty-handler "4.1.0.Beta4"]
                 [org.apache.httpcomponents/httpclient "4.5"]
                 [org.apache.httpcomponents/httpmime "4.5"]
                 [org.javassist/javassist "3.19.0-GA"]]
  :profiles {:performance
             {:main meerkat.examples.rest
              :jvm-opts ["-Xms512m" "-Xmx512m" "-XX:+UseG1GC" "-XX:+UseCompressedOops" "-XX:+UnlockCommercialFeatures" "-XX:+FlightRecorder"]}})
