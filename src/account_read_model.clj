(ns account-read-model
  (:gen-class))

(defn deposit-amount [account {amount    :amount
                               timestamp :timestamp}]
  (dosync (alter account
                 (fn [account amount] (update account :balance + amount))
                 amount)
          (alter account
                 (fn [account timestamp] (assoc account :last-updated timestamp))
                 timestamp)))

(defn withdraw-amount [account {amount    :amount
                                timestamp :timestamp}]
  (dosync (alter account
                 (fn [account amount] (update account :balance - amount))
                 amount)
          (alter account
                 (fn [account timestamp] (assoc account :last-updated timestamp))
                 timestamp)))

(def event-handlers {
  :amount-deposited deposit-amount
  :amount-withdrawn withdraw-amount
})

(defn update-account [account]
  (fn [event]
    ; Consider writing a macro for this
    ; (when-let with multiple bindings, preferably short-circuiting)
    (when (= (event :account-id) (account :id))
      (when-let [event-handler (event-handlers (event :type))]
        (event-handler account event)))))


(defn create-account [event-store id name current-time]
  (let [account (ref {:id           id
                      :name         name
                      :balance      0
                      :last-updated current-time})]
    ((event-store :subscribe) (update-account account))
    account))
