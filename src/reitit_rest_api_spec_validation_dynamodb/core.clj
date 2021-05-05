(ns reitit-rest-api-spec-validation-dynamodb.core
  (:require [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.coercion :as coercion]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]
            ;; Uncomment to use
            ; [reitit.ring.middleware.dev :as dev]
            ; [reitit.ring.spec :as spec]
            ; [spec-tools.spell :as spell]
            [ring.adapter.jetty :as jetty]
            [muuntaja.core :as m]
            [clojure.spec.alpha :as s]
            [taoensso.faraday :as far]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]))

(def dyn-client-opts
  {:endpoint "http://localhost:8000" ; For DynamoDB Local
   ;; :endpoint "http://dynamodb.eu-west-1.amazonaws.com" ; For EU West 1 AWS region
   :access-key "<AWS_DYNAMODB_ACCESS_KEY>"
   :secret-key "<AWS_DYNAMODB_SECRET_KEY>"})

(s/def ::customerid string?)
(s/def ::timestamp number?)
(s/def ::date string?)
(s/def ::currency string?)

(s/def ::desc string?)
(s/def ::qty int?)
(s/def ::amount int?)

(s/def ::txn (s/keys :req-un [::desc ::qty ::amount]))
(s/def ::txns (s/coll-of ::txn))

(s/def ::txn-get-request (s/keys :req-un [::customerid ::timestamp]))
(s/def ::txn-get-response (s/keys :req-un [::customerid ::timestamp ::date ::currency ::txns]))

(s/def ::txn-save-request (s/keys :req-un [::customerid ::date ::currency ::txns]))
(s/def ::txn-save-response (s/keys :req-un [::customerid ::timestamp]))

(s/def ::txn-update-request (s/keys :req-un [::customerid ::timestamp ::date ::currency ::txns]))
(s/def ::txn-update-response (s/keys :req-un [::customerid ::timestamp]))


(def app
  (ring/ring-handler
   (ring/router
    [["/swagger.json"
      {:get {:no-doc true
             :swagger {:info {:title "my-api"}}
             :handler (swagger/create-swagger-handler)}}]

     ["/transactions"
      {:get {:summary "retrieve list-of-transactions"
             :parameters {:query ::txn-get-request}
             :responses {200 {:body ::txn-get-response}}
             :handler (fn [{{{:keys [customerid timestamp]} :query} :parameters}]
                        (let [db-result (far/get-item dyn-client-opts :transactions {:customerid customerid :timestamp timestamp})]
                          {:status 200
                           :body db-result}))}
       :post {:summary "save list-of-transactions"
              :parameters {:body ::txn-save-request}
              :responses {200 {:body ::txn-save-response}}
              :handler (fn [{{{:keys [customerid date currency txns]} :body} :parameters}]
                         (let [timestamp (tc/to-long (time/now))]
                           (far/put-item dyn-client-opts
                                         :transactions
                                         {:customerid customerid :timestamp timestamp
                                          :date date :currency currency
                                          :txns (far/freeze txns)})
                           {:status 200
                            :body {:customerid customerid :timestamp timestamp}}))}
       :put {:summary "update list-of-transactions"
             :parameters {:body ::txn-update-request}
             :responses {200 {:body ::txn-update-response}}
             :handler (fn [{{{:keys [customerid timestamp date currency txns]} :body} :parameters}]
                        (far/put-item dyn-client-opts
                                      :transactions
                                      {:customerid customerid :timestamp timestamp
                                       :date date :currency currency
                                       :txns (far/freeze txns)})
                        {:status 200
                         :body {:customerid customerid :timestamp timestamp}})}}]]

    {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
       ;;:validate spec/validate ;; enable spec validation for route data
       ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
     :exception pretty/exception
     :data {:coercion reitit.coercion.spec/coercion
            :muuntaja m/instance
            :middleware [;; swagger feature
                         swagger/swagger-feature
                           ;; query-params & form-params
                         parameters/parameters-middleware
                           ;; content-negotiation
                         muuntaja/format-negotiate-middleware
                           ;; encoding response body
                         muuntaja/format-response-middleware
                           ;; exception handling
                         exception/exception-middleware
                           ;; decoding request body
                         muuntaja/format-request-middleware
                           ;; coercing response bodys
                         coercion/coerce-response-middleware
                           ;; coercing request parameters
                         coercion/coerce-request-middleware
                           ;; multipart
                         multipart/multipart-middleware]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/"
      :config {:validatorUrl nil
               :operationsSorter "alpha"}})
    (ring/create-default-handler))))

(defn start []
  (jetty/run-jetty #'app {:port 3000, :join? false})
  (println "server running in port 3000"))

(defn -main []
  (start))
