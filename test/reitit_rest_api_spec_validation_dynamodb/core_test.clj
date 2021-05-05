(ns reitit-rest-api-spec-validation-dynamodb.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [reitit-rest-api-spec-validation-dynamodb.core :refer [app]]
            [cheshire.core :as cheshire]
            [ring.mock.request :refer [request json-body]]))

(def SAMPLE-POST-REQUEST-BODY
  {"customerid" "77777"
   "date" "Tue, 04 May 2021 23:24:23 GMT"
   "currency" "USD"
   "txns" [{"desc" "hockey-stick", "qty" 1, "amount" 4000}]})

(def SAMPLE-PUT-REQUEST-BODY
  (assoc SAMPLE-POST-REQUEST-BODY :currency "GBP"))

(defn parse-json [body] (cheshire/parse-string body true))


(deftest reitit-rest-api-spec-validation-dynamodb

  (testing "Transactions - POST, GET & PUT requests"
    ;; create a table-item & verify that the expected hash-key is returned
    (let [{:keys [customerid timestamp]} (-> (request :post "/transactions")
                                             (json-body SAMPLE-POST-REQUEST-BODY)
                                             app :body slurp parse-json)]
      (is (= customerid "77777"))

      ;; retrieve the table-item and verify the expected currency (USD)
      (let [full-query-url (str "/transactions?customerid=77777&timestamp=" timestamp)
            txn-get-result (-> (request :get full-query-url)
                               app :body slurp parse-json)]
        (is (= (:currency txn-get-result) "USD"))

        ;; update the table-item (with GBP currency); verify that timestamp is the same
        (let [txn-update-result (-> (request :put "/transactions")
                                    (json-body (assoc SAMPLE-PUT-REQUEST-BODY :timestamp timestamp))
                                    app :body slurp parse-json)]
          (is (= (:timestamp txn-update-result) timestamp)))

        ;; retrieve the table-item a second time; verify currency was updated.
        (let [txn-get-result2 (-> (request :get full-query-url)
                                  app :body slurp parse-json)]
          (is (= (:currency txn-get-result2) "GBP")))))))