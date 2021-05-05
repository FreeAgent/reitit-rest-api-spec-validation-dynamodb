(defproject reitit-rest-api-spec-validation-dynamodb "0.1.0-SNAPSHOT"
  :description "Sample REST API, using Reitit, with spec for validation, plus DynamoDB (Local) for persistence"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.taoensso/faraday "1.11.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [metosin/reitit "0.5.13"]
                 [clj-time "0.15.2"]]
  :profiles {:dev {:dependencies [[ring/ring-mock "0.3.2"]
                                  [midje "1.9.10"]
                                  [cheshire "5.10.0"]]}}
  :repl-options {:init-ns reitit-rest-api-spec-validation-dynamodb.core}
  :main reitit-rest-api-spec-validation-dynamodb.core)
