(ns reserve-read-model
  (:gen-class))


(defn deposit-amount [reserve {amount    :amount
                               timestamp :timestamp}]
  (dosync (alter reserve
                 (fn [reserve amount] (update reserve :amount + amount))
                 amount)
          (alter reserve
                 (fn [reserve timestamp] (assoc reserve :last-updated timestamp))
                 timestamp)))

(defn withdraw-amount [reserve {amount    :amount
                                timestamp :timestamp}]
  (dosync (alter reserve
                 (fn [reserve amount] (update reserve :amount - amount))
                 amount)
          (alter reserve
                 (fn [reserve timestamp] (assoc reserve :last-updated timestamp))
                 timestamp)))

(def event-handlers {
  :amount-deposited deposit-amount
  :amount-withdrawn withdraw-amount
})


(defn update-reserve [reserve]
  (fn [event]
    (when-let [event-handler (event-handlers (event :type))]
      (event-handler reserve event))))


(defn create-reserve-read-model [event-store]
  (let [reserve (ref {:amount       0
                      :last-updated (java.util.Date.)})]
    ((event-store :subscribe) (update-reserve reserve))
    reserve))
