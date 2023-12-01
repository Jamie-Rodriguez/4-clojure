(ns http-server-test
  (:require [clojure.test       :refer [deftest is testing]]
            [clojure.instant    :refer [read-instant-date]]
            [event-store        :refer [use-in-memory-event-store]]
            [reserve-read-model :refer [create-reserve-read-model]]
            [http-server        :refer [handle-get-account
                                        handle-get-reserve
                                        handle-create-account
                                        handle-deposit
                                        handle-withdrawal]]))

(defn http-server-tests [event-store
                         account-write-models
                         account-read-models
                         reserve-read-model]
  (let [id "my-test-id"
        ; Because handle-create-account takes in an ID as a string and then
        ; converts it into a keyword; when querying the read-models, we need to
        ; query using the keyword form of the ID.
        ; The string form of the ID is also used in the request URLs for the
        ; handlers
        keyword-id              (keyword id)
        account-creation-time   (read-instant-date "2000-01-01T00:00+00:00")
        deposit-time            (read-instant-date "2001-01-01T00:00+00:00")
        withdrawal-time         (read-instant-date "2002-01-01T00:00+00:00")
        account-creation-time-2 (read-instant-date "2003-01-01T00:00+00:00")]

    (testing "Creating an account with ID creates a write and a read model"
      (let [request  {:uri    (str "/account/" id)
                      :params {:id   id
                               :name "Mr Test Account"}}
            response (handle-create-account (fn [] (str (random-uuid)))
                                            (fn [] account-creation-time)
                                            event-store
                                            account-write-models
                                            account-read-models
                                            request)]

        (is (= (response :status) 201))

        (is (contains? (deref account-write-models) keyword-id))
        (is (contains? (deref account-read-models) keyword-id))

        (is (= ((deref reserve-read-model) :amount) 0))))

    (testing "Depositing a valid amount to a valid account is successful"
      (let [request  {:uri    (str "/account/deposit/" id)
                      :params {:id          keyword-id
                               :description "A valid deposit"
                               :amount      100}}
            response (handle-deposit (fn [] (str (random-uuid)))
                                     (fn [] deposit-time)
                                     event-store
                                     request)
            write-models-snapshot (deref account-write-models)
            write-model-snapshot  (deref (write-models-snapshot keyword-id))
            read-models-snapshot  (deref account-read-models)
            read-model-snapshot   (deref (read-models-snapshot keyword-id))]

        (is (= (response :status) 200))

        (is (= (write-model-snapshot :balance) 100))

        (is (= (read-model-snapshot :balance) 100))
        (is (= (read-model-snapshot :last-updated) deposit-time))

        (is (= (deref reserve-read-model)
               {:amount       100
                :last-updated deposit-time}))))

    (testing "Depositing an invalid amount to a valid account is not successful"
      (let [request  {:uri    (str "/account/deposit/" id)
                      :params {:id          keyword-id
                               :description "An invalid deposit"
                               :amount      -1000}}
            response (handle-deposit (fn [] (str (random-uuid)))
                                     (fn [] deposit-time)
                                     event-store
                                     request)
            write-models-snapshot (deref account-write-models)
            write-model-snapshot  (deref (write-models-snapshot keyword-id))
            read-models-snapshot  (deref account-read-models)
            read-model-snapshot   (deref (read-models-snapshot keyword-id))]

        (is (= (response :status) 400))

        (is (= (write-model-snapshot :balance) 100))

        (is (= (read-model-snapshot :balance) 100))
        (is (= (read-model-snapshot :last-updated) deposit-time))

        (is (= (deref reserve-read-model)
               {:amount       100
                :last-updated deposit-time}))))

    (testing "Depositing a valid amount to a non-existent account is not successful"
      (let [random-id (str (random-uuid))
            request   {:uri    (str "/account/deposit/" random-id)
                       :params {:id           random-id
                                :description "A valid deposit, but to an invalid account"
                                :amount      100}}
            response  (handle-deposit (fn [] (str (random-uuid)))
                                      (fn [] deposit-time)
                                      event-store
                                      request)
            write-models-snapshot (deref account-write-models)
            write-model-snapshot  (deref (write-models-snapshot keyword-id))
            read-models-snapshot  (deref account-read-models)
            read-model-snapshot   (deref (read-models-snapshot keyword-id))]

        (is (= (response :status) 400))

        (is (= (write-model-snapshot :balance) 100))

        (is (= (read-model-snapshot :balance) 100))
        (is (= (read-model-snapshot :last-updated) deposit-time))

        (is (= (deref reserve-read-model)
               {:amount       100
                :last-updated deposit-time}))))

    (testing "Creating an account with a pre-existing ID is not successful"
      (let [request  {:uri    (str "/account/" id)
                      :params {:id   id
                               :name "Duplicate account"}}
            response (handle-create-account (fn [] (str (random-uuid)))
                                            (fn [] (read-instant-date "2002-01-01T00:00+00:00"))
                                            event-store
                                            account-write-models
                                            account-read-models
                                            request)
            write-models-snapshot (deref account-write-models)
            write-model-snapshot  (deref (write-models-snapshot keyword-id))
            read-models-snapshot  (deref account-read-models)
            read-model-snapshot   (deref (read-models-snapshot keyword-id))]

        (is (= (response :status) 400))

        (is (= (-> write-models-snapshot keys count) 1))
        (is (= (write-model-snapshot :balance) 100))

        (is (= (-> read-models-snapshot keys count) 1))
        (is (= (read-model-snapshot :balance) 100))
        (is (= (read-model-snapshot :last-updated) deposit-time))

        (is (= (deref reserve-read-model)
               {:amount       100
                :last-updated deposit-time}))))

    (testing "Withdrawing a valid amount from a non-existent account is not successful"
      (let [random-id (str (random-uuid))
            request   {:uri    (str "/account/withdraw/" random-id)
                       :params {:id          random-id
                                :description "A valid withdrawal amount from a non-existent account"
                                :amount      30}}
            response  (handle-withdrawal (fn [] (str (random-uuid)))
                                         (fn [] deposit-time)
                                         event-store
                                         account-write-models
                                         request)
            write-models-snapshot (deref account-write-models)
            write-model-snapshot  (deref (write-models-snapshot keyword-id))
            read-models-snapshot  (deref account-read-models)
            read-model-snapshot   (deref (read-models-snapshot keyword-id))]

        (is (= (response :status) 400))

        (is (= (write-model-snapshot :balance) 100))

        (is (= (read-model-snapshot :balance) 100))
        (is (= (read-model-snapshot :last-updated) deposit-time))

        (is (= (deref reserve-read-model)
               {:amount       100
                :last-updated deposit-time}))))

    (testing "Withdrawing an invalid amount from an existing account is not successful"
      (let [request   {:uri    (str "/account/withdraw/" id)
                       :params {:id          keyword-id
                                :description "An invalid withdrawal amount from a existing account"
                                :amount      1000000}}
            response  (handle-withdrawal (fn [] (str (random-uuid)))
                                         (fn [] withdrawal-time)
                                         event-store
                                         account-write-models
                                         request)
            write-models-snapshot (deref account-write-models)
            write-model-snapshot  (deref (write-models-snapshot keyword-id))
            read-models-snapshot  (deref account-read-models)
            read-model-snapshot   (deref (read-models-snapshot keyword-id))]

        (is (= (response :status) 400))

        (is (= (write-model-snapshot :balance) 100))

        (is (= (read-model-snapshot :balance) 100))
        (is (= (read-model-snapshot :last-updated) deposit-time))

        (is (= (deref reserve-read-model)
               {:amount       100
                :last-updated deposit-time}))))

    (testing "Withdrawing a valid amount from an existing account is successful"
      (let [request   {:uri    (str "/account/withdraw/" id)
                       :params {:id          id
                                :description "A valid withdrawal amount from a existing account"
                                :amount      30}}
            response  (handle-withdrawal (fn [] (str (random-uuid)))
                                         (fn [] withdrawal-time)
                                         event-store
                                         account-write-models
                                         request)
            write-models-snapshot (deref account-write-models)
            write-model-snapshot  (deref (write-models-snapshot keyword-id))
            read-models-snapshot  (deref account-read-models)
            read-model-snapshot   (deref (read-models-snapshot keyword-id))]

        (is (= (response :status) 200))

        (is (= (write-model-snapshot :balance) 70))

        (is (= (read-model-snapshot :balance) 70))
        (is (= (read-model-snapshot :last-updated) withdrawal-time))

        (is (= (deref reserve-read-model)
               {:amount       70
                :last-updated withdrawal-time}))))

    (testing "Querying the reserve returns the correct amount"
      (let [response (handle-get-reserve reserve-read-model)]
        (is (= (response :status) 200))
        (is (= (response :body)
               {:amount 70 :last-updated withdrawal-time}))))

    (testing "Creating an account without providing an ID creates a write and a read model"
      (let [id-2     (str (random-uuid))
            request  {:uri    "/account/"
                      :params {:name "Another account"}}
            ; Note in this test case, handle-create-account will call it's
            ; generate-id function twice.
            ; (first for generating the account-id, and second for the event-id)
            response (handle-create-account (fn [] id-2)
                                            (fn [] account-creation-time-2)
                                            event-store
                                            account-write-models
                                            account-read-models
                                            request)]

        (is (= (response :status) 201))

        (is (= (-> account-write-models deref keys count) 2))
        (is (contains? (deref account-write-models) (keyword id-2)))

        (is (= (-> account-read-models deref keys count) 2))
        (is (contains? (deref account-read-models) (keyword id-2)))

        (is (= (deref reserve-read-model)
               {:amount       70
                :last-updated withdrawal-time}))))

    (testing "Querying for a non-existent account is not successful"
      (let [id-2     (str (random-uuid))
            request  {:uri    (str "/account/" id-2)
                      :params {:id id-2}}
            response (handle-get-account account-read-models request)]
        (is (= (response :status) 404))))

    (testing "Querying for an existing account is successful"
      (let [request  {:uri    (str "/account/" id)
                      :params {:id keyword-id}}
            response (handle-get-account account-read-models request)]
        (is (= (response :status) 200))
        (is (= (response :body)
               (-> account-read-models deref keyword-id deref)))))))


(deftest in-memory-http-server-tests
  (let [event-store          (use-in-memory-event-store)
        account-write-models (ref {})
        account-read-models  (ref {})
        reserve-read-model   (create-reserve-read-model event-store)]
    (http-server-tests event-store
                       account-write-models
                       account-read-models
                       reserve-read-model)))
