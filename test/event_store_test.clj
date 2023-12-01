(ns event-store-test
  (:require [clojure.test :refer [deftest is testing]]
            [event-store  :refer [use-in-memory-event-store]]))

(defn event-store-tests [event-store]
  (let [account-name  "my test account"
        account-id    (random-uuid)
        event-1       {:type :account-created
                       :name account-name}
        event-2       {:type        :amount-deposited
                       :id          (random-uuid)
                       :account-id  account-id
                       :description "donation from Bill Gates"
                       :amount      10000}
        event-3       {:type        :amount-withdrawn
                       :id          (random-uuid)
                       :account-id  account-id
                       :description "parking - $7.50"
                       :amount      750}
        events        [event-1 event-2 event-3]
        stored-events (map-indexed (fn [i event] (assoc event :position i))
                                   events)]

    (testing "read empty event store"
      (is (= ((event-store :read) 0)
             []))
      (is (= ((event-store :num-events)) 0)))

    (testing "read subset of non-empty event store"
      (doseq [event events] ((event-store :add) event))
      (is (= ((event-store :read) 1)
             [(assoc event-2 :position 1)
              (assoc event-3 :position 2)]))
      (is (= ((event-store :num-events)) 3)))

    (testing "read all of non-empty event store"
      (is (= ((event-store :read) 0)
             stored-events))
      (is (= ((event-store :num-events)) 3)))

    (testing "observers are notified when an event is added"
      (let [result         (atom 0)
            observer       (fn [{data :payload}] (reset! result data))
            expected-value 123
            event          {:type    :test-observer-event
                            :payload expected-value}]
        ((event-store :subscribe) observer)
        ((event-store :add) event)
        (is (= @result expected-value))))))


(deftest in-memory-event-store-tests
         (event-store-tests (use-in-memory-event-store)))
