(ns account-read-model-test
  (:require [clojure.test       :refer [deftest is testing]]
            [clojure.instant    :refer [read-instant-date]]
            [event-store        :refer [use-in-memory-event-store]]
            [account-read-model :as    read-model]))

(defn account-read-model-tests [event-store]
  (let [id              (random-uuid)
        name            "test account"
        creation-time   (read-instant-date "1970-01-01T00:00+00:00")
        deposit-time    (read-instant-date "1970-01-02T00:00+00:00")
        withdrawal-time (read-instant-date "1970-01-03T00:00+00:00")
        account         (read-model/create-account event-store
                                                   id
                                                   name
                                                   creation-time)]

    (testing "create account read-model"
      (is (= ((deref account) :id) id))
      (is (= ((deref account) :name) name))
      (is (zero? ((deref account) :balance)))
      (is (= ((deref account) :last-updated) creation-time)))

    (testing "account does not update when receiving events for different IDs"
      ((event-store :add) {:type       :amount-deposited
                           :event-id   (random-uuid)
                           :account-id (random-uuid)
                           :amount     999
                           :timestamp  deposit-time})
      ((event-store :add) {:type       :amount-withdrawn
                           :event-id   (random-uuid)
                           :account-id (random-uuid)
                           :amount     900
                           :timestamp  deposit-time})
      (is (zero? ((deref account) :balance)))
      (is (= ((deref account) :last-updated) creation-time)))

    (testing "account updates when receiving valid \"amount deposited\" event"
      ((event-store :add) {:type       :amount-deposited
                           :event-id   (random-uuid)
                           :account-id id
                           :amount     456
                           :timestamp  deposit-time})
      (is (= ((deref account) :balance) 456))
      (is (= ((deref account) :last-updated) deposit-time)))

    (testing "account updates when receiving valid \"amount withdrawn\" event"
      ((event-store :add) {:type       :amount-withdrawn
                           :event-id   (random-uuid)
                           :account-id id
                           :amount     123
                           :timestamp  withdrawal-time})
      (is (= ((deref account) :balance) 333))
      (is (= ((deref account) :last-updated) withdrawal-time)))

    (testing "account does not update when receiving unknown event"
      ((event-store :add) {:type       :some-other-event
                           :account-id id
                           :timestamp  (read-instant-date "1990-01-01T00:00+00:00")})
      (is (= ((deref account) :balance) 333))
      (is (= ((deref account) :last-updated) withdrawal-time)))))


(deftest in-memory-account-read-model-tests
  (account-read-model-tests (use-in-memory-event-store)))
