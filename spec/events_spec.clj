(ns events-spec
  (:require [clojure.spec.alpha :as s]))

(s/def :account-creation-rejected/type #{:account-creation-rejected})
(s/def :account-creation-rejected/timestamp inst?)
(s/def :account-creation-rejected/event-id uuid?)
(s/def :account-creation-rejected/name string?)
(s/def :account-creation-rejected/event
  (s/keys :req-un [:account-creation-rejected/type
                   :account-creation-rejected/timestamp
                   :account-creation-rejected/event-id
                   :account-creation-rejected/name]))

(s/def :account-created/type #{:account-created})
(s/def :account-created/timestamp inst?)
(s/def :account-created/event-id uuid?)
(s/def :account-created/name string?)
(s/def :account-created/event
  (s/keys :req-un [:account-created/type
                   :account-created/timestamp
                   :account-created/event-id
                   :account-created/name]))

(s/def :deposit-rejected/type #{:deposit-rejected})
(s/def :deposit-rejected/timestamp inst?)
(s/def :deposit-rejected/event-id uuid?)
(s/def :deposit-rejected/account-id uuid?)
(s/def :deposit-rejected/decription string?)
(s/def :deposit-rejected/amount pos-int?) ;; cents, not dollars!
(s/def :deposit-rejected/event
  (s/keys :req-un [:deposit-rejected/type
                   :deposit-rejected/event-id
                   :deposit-rejected/timestamp
                   :deposit-rejected/account-id
                   :deposit-rejected/amount]
          :opt-un [:deposit-rejected/decription]))

(s/def :amount-deposited/type #{:amount-deposited})
(s/def :amount-deposited/timestamp inst?)
(s/def :amount-deposited/event-id uuid?)
(s/def :amount-deposited/account-id uuid?)
(s/def :amount-deposited/decription string?)
(s/def :amount-deposited/amount pos-int?) ;; cents, not dollars!
(s/def :amount-deposited/event
  (s/keys :req-un [:amount-deposited/type
                   :amount-deposited/timestamp
                   :amount-deposited/event-id
                   :amount-deposited/account-id
                   :amount-deposited/amount]
          :opt-un [:amount-deposited/decription]))

(s/def :withdrawal-rejected/type #{:withdrawal-rejected})
(s/def :withdrawal-rejected/timestamp inst?)
(s/def :withdrawal-rejected/event-id uuid?)
(s/def :withdrawal-rejected/account-id uuid?)
(s/def :withdrawal-rejected/decription string?)
(s/def :withdrawal-rejected/amount pos-int?) ;; cents, not dollars!
(s/def :withdrawal-rejected/event
  (s/keys :req-un [:withdrawal-rejected/type
                   :withdrawal-rejected/timestamp
                   :withdrawal-rejected/event-id
                   :withdrawal-rejected/account-id
                   :withdrawal-rejected/amount]
          :opt-un [:withdrawal-rejected/decription]))

(s/def :amount-withdrawn/type #{:amount-withdrawn})
(s/def :amount-withdrawn/timestamp inst?)
(s/def :amount-withdrawn/event-id uuid?)
(s/def :amount-withdrawn/account-id uuid?)
(s/def :amount-withdrawn/decription string?)
(s/def :amount-withdrawn/amount pos-int?) ;; cents, not dollars!
(s/def :amount-withdrawn/event
  (s/keys :req-un [:amount-withdrawn/type
                   :amount-withdrawn/timestamp
                   :amount-withdrawn/event-id
                   :amount-withdrawn/account-id
                   :amount-withdrawn/amount]
          :opt-un [:amount-withdrawn/decription]))
