(ns account-commands-test
  (:require [clojure.test     :refer [deftest is testing]]
            [utils            :refer [submap?]]
            [event-store      :refer [use-in-memory-event-store]]
            [account-commands :as    commands]))

(defn account-commands-tests [event-store]
  (let [id               (keyword (str (random-uuid)))
        name             "test account"
        [creation-result
         account
         read-model]     (commands/create-account event-store
                                                  id
                                                  name
                                                  (random-uuid)
                                                  (java.util.Date.))]

    (testing "create account command"
      (is (= creation-result :account-created))
      (is (= ((event-store :num-events)) 1))

      (is (= ((deref account) :id) id))
      (is (= ((deref account) :name) name))
      (is (zero? ((deref account) :balance)))

      (is (zero? ((deref read-model) :balance))))

    (testing "create account command for a pre-existing ID creates rejection event"
      (is (= (commands/create-account event-store
                                      id
                                      name
                                      (random-uuid)
                                      (java.util.Date.))
             [:account-creation-rejected]))
      (is (= ((event-store :num-events)) 2))

      (is (submap? {:type :account-creation-rejected}
                   (last ((event-store :read) 0)))))

    (testing "deposit into non-existent account creates rejection event"
      (is (= (commands/deposit-into-account event-store
                                            {:account-id  (random-uuid)
                                             :description "A deposit into an account that doesn't exist"
                                             :amount      1234
                                             :event-id    (random-uuid)
                                             :timestamp   (java.util.Date.)})
             :deposit-rejected))
      (is (= ((event-store :num-events)) 3))

      (is (submap? {:type :deposit-rejected}
                   (last ((event-store :read) 0)))))

    (testing "withdrawing from non-existent account creates rejection event"
      (is (= (commands/withdraw-from-account event-store
                                             account
                                             {:account-id  (random-uuid)
                                              :description "A withdrawal from an account that doesn't exist"
                                              :amount      3
                                              :event-id    (random-uuid)
                                              :timestamp   (java.util.Date.)})
             :withdrawal-rejected))
      (is (= ((event-store :num-events)) 4))

      (is (submap? {:type :withdrawal-rejected}
                   (last ((event-store :read) 0)))))

    (testing "account does not update when receiving a negative deposit amount"
      (is (= (commands/deposit-into-account event-store
                                            {:account-id  id
                                             :description "this is an invalid deposit"
                                             :amount      -123
                                             :event-id    (random-uuid)
                                             :timestamp   (java.util.Date.)})
             :deposit-rejected))
      (is (= ((event-store :num-events)) 5))

      (is (submap? {:type :deposit-rejected}
                   (last ((event-store :read) 0))))
      (is (= ((deref account) :balance) 0)))

    (testing "account updates when receiving a valid deposit amount"
      (is (= (commands/deposit-into-account event-store
                                            {:account-id  id
                                             :description "this is a valid deposit"
                                             :amount      210
                                             :event-id    (random-uuid)
                                             :timestamp   (java.util.Date.)})
             :amount-deposited))
      (is (= ((event-store :num-events)) 6))

      (is (submap? {:type :amount-deposited}
                   (last ((event-store :read) 0))))
      (is (= ((deref account) :balance) 210)))

    (testing "account does not update when receiving a negative withdrawal amount"
      (is (= (commands/withdraw-from-account event-store
                                             account
                                             {:account-id  id
                                              :description "this is an invalid withdrawal"
                                              :amount      -123
                                              :event-id    (random-uuid)
                                              :timestamp   (java.util.Date.)})
             :withdrawal-rejected))
      (is (= ((event-store :num-events)) 7))

      (is (submap? {:type :withdrawal-rejected}
                   (last ((event-store :read) 0))))
      (is (= ((deref account) :balance) 210)))

    (testing "account does not update when withdrawing over the account balance amount"
      (is (= (commands/withdraw-from-account event-store
                                             account
                                             {:account-id  id
                                              :description "this is a withdrawal way over the limit"
                                              :amount      100000000
                                              :event-id    (random-uuid)
                                              :timestamp   (java.util.Date.)})
             :withdrawal-rejected))
      (is (= ((event-store :num-events)) 8))

      (is (submap? {:type :withdrawal-rejected}
                   (last ((event-store :read) 0))))
      (is (= ((deref account) :balance) 210)))

    (testing "account updates when receiving a valid withdrawal amount"
      (is (= (commands/withdraw-from-account event-store
                                             account
                                             {:account-id  id
                                              :description "this is a valid withdrawal"
                                              :amount      110
                                              :event-id    (random-uuid)
                                              :timestamp   (java.util.Date.)})
             :amount-withdrawn))
      (is (= ((event-store :num-events)) 9))

      (is (submap? {:type :amount-withdrawn}
                   (last ((event-store :read) 0))))
      (is (= ((deref account) :balance) 100)))))


(deftest in-memory-account-commands-tests
  (account-commands-tests (use-in-memory-event-store)))
