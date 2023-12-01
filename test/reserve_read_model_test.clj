(ns reserve-read-model-test
  (:require [clojure.test       :refer [deftest is testing]]
            [clojure.instant    :refer [read-instant-date]]
            [event-store        :refer [use-in-memory-event-store]]
            [reserve-read-model :refer [create-reserve-read-model]]))

(defn reserve-read-model-tests [event-store]
  (let [reserve (create-reserve-read-model event-store)
        deposit-event-1    {:type       :amount-deposited
                            :event-id   (random-uuid)
                            :account-id (random-uuid)
                            :amount     123
                            :timestamp  (read-instant-date "1991-12-03T19:19+08:00")}
        deposit-event-2    {:type       :amount-deposited
                            :event-id   (random-uuid)
                            :account-id (random-uuid)
                            :amount     456
                            :timestamp  (read-instant-date "1991-12-25T00:00+08:00")}
        withdrawal-event   {:type       :amount-withdrawn
                            :event-id   (random-uuid)
                            :account-id (random-uuid)
                            :amount     79
                            :timestamp  (read-instant-date "1991-12-31T23:59+08:00")}]

    (testing "reserve listens to event store and aggregates all deposits and withdrawals"
      (doseq [event [deposit-event-1 deposit-event-2 withdrawal-event]]
        ((event-store :add) event))
      (is (= ((deref reserve) :last-updated)
             (read-instant-date "1991-12-31T23:59+08:00")))
      (is (= ((deref reserve) :amount) 500)))))


(deftest in-memory-reserve-read-model-tests
  (reserve-read-model-tests (use-in-memory-event-store)))
