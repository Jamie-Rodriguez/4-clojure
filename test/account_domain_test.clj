(ns account-domain-test
  (:require [clojure.test   :refer [deftest is testing]]
            [event-store    :refer [use-in-memory-event-store]]
            [account-domain :as    domain]))

(defn account-domain-tests [event-store]
  (let [id      (random-uuid)
        name    "test account"
        account (domain/create-account event-store id name)]

    (testing "create account"
      (is (= ((deref account) :id) id))
      (is (= ((deref account) :name) name))
      (is (zero? ((deref account) :balance))))

    (testing "account does not update when receiving events for different IDs"
      ((event-store :add) {:type       :amount-deposited
                           :event-id   (random-uuid)
                           :account-id (random-uuid)
                           :amount     999})
      ((event-store :add) {:type       :amount-withdrawn
                           :event-id   (random-uuid)
                           :account-id (random-uuid)
                           :amount     900})
      (is (zero? ((deref account) :balance))))

    (testing "account updates when receiving valid \"amount deposited\" event"
      ((event-store :add) {:type       :amount-deposited
                           :event-id   (random-uuid)
                           :account-id id
                           :amount     456})
      (is (= ((deref account) :balance) 456)))

    (testing "account updates when receiving valid \"amount withdrawn\" event"
      ((event-store :add) {:type       :amount-withdrawn
                           :event-id   (random-uuid)
                           :account-id id
                           :amount     123})
      (is (= ((deref account) :balance) 333)))

    (testing "account does not update when receiving unknown event"
      ((event-store :add) {:type       :some-other-event
                           :account-id id})
      (is (= ((deref account) :balance) 333)))))


(deftest in-memory-account-domain-tests
  (account-domain-tests (use-in-memory-event-store)))
