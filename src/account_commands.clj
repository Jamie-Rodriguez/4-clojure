(ns account-commands
  (:require [account-domain     :as domain]
            [account-read-model :as read-model])
  (:gen-class))


(defn account-exists [event-store account-id]
  (->> ((event-store :read) 0)
       (filter (fn [event] (and (= (event :type) :account-created)
                                (= (event :account-id) account-id))))
       count
       pos?))


(defn create-account [event-store account-id name event-id timestamp]
  (let [account-doesnt-exist (not (account-exists event-store account-id))]
    ((event-store :add) {:type       (if account-doesnt-exist
                                         :account-created
                                         :account-creation-rejected)
                         :event-id   event-id
                         :account-id account-id
                         :name       name
                         :timestamp  timestamp})
    (if account-doesnt-exist
      (do (println "Created account, ID:" account-id ", name:" name "at" timestamp)
          [:account-created
           (domain/create-account event-store account-id name)
           (read-model/create-account event-store account-id name timestamp)])
      [:account-creation-rejected])))

(defn deposit-into-account [event-store
                            {account-id  :account-id
                             description :description
                             amount      :amount
                             event-id    :event-id
                             timestamp   :timestamp}]
  (let [valid-deposit (and (pos? amount)
                           (account-exists event-store account-id))]
    ((event-store :add) {:type        (if valid-deposit
                                          :amount-deposited
                                          :deposit-rejected)
                         :event-id    event-id
                         :account-id  account-id
                         :description description
                         :amount      amount
                         :timestamp   timestamp})
    (if valid-deposit :amount-deposited :deposit-rejected)))

(defn withdraw-from-account [event-store
                             account
                             {account-id  :account-id
                              description :description
                              amount      :amount
                              event-id    :event-id
                              timestamp   :timestamp}]
  (let [valid-withdrawal (and (account-exists event-store account-id)
                              (pos? amount)
                              (<= amount ((deref account) :balance)))]
    ((event-store :add)
       {:type        (if valid-withdrawal
                       :amount-withdrawn
                       :withdrawal-rejected)
        :event-id    event-id
        :account-id  account-id
        :description description
        :amount      amount
        :timestamp   timestamp})
    (if valid-withdrawal :amount-withdrawn :withdrawal-rejected)))
