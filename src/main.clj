(ns main
  (:require [event-store        :refer [use-in-memory-event-store]]
            [reserve-read-model :refer [create-reserve-read-model]]
            [http-server        :refer [start-app]])
  (:gen-class))

(defn -main []
  (def event-store (use-in-memory-event-store))
  (def account-write-models (ref {}))
  (def account-read-models (ref {}))
  (def reserve-read-model (create-reserve-read-model event-store))

  (start-app {:event-store          event-store
              :account-write-models account-write-models
              :account-read-models  account-read-models
              :reserve-read-model   reserve-read-model}))
