(ns event-store
  (:gen-class))

(defn use-in-memory-event-store []
  (let [store     (ref [])
        observers (ref [])]
    {:subscribe (fn [observer]
                  (dosync (alter observers conj observer)))
     ; Only adds one event at a time
     ; Consider being able to add multiple events
     ; Though this affects the observer code
     :add (fn [event]
            (let [stored-event (assoc event :position (count (deref store)))]
              (dosync (alter store conj stored-event))
              (doseq [observer (deref observers)]
                (observer stored-event))))
     ; :read is 0-indexed
     :read (fn [position] (nthrest (deref store) position))
     :num-events #(count (deref store))}))
