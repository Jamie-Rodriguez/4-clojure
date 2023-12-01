(ns http-server
  (:require [ring.adapter.jetty             :refer [run-jetty]]
            [ring.middleware.json           :refer [wrap-json-params
                                                    wrap-json-response]]
            [ring.middleware.case-format    :refer [wrap->kebab->snake]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [compojure.core                 :refer [routes GET POST]]
            [compojure.route                :as    route]
            [ring.util.response             :as    resp]
            [account-commands               :as    commands])
  (:gen-class))


(defn- get-current-time [] (java.util.Date.))

(defn- generate-id [] (str (random-uuid)))


(defn handle-get-account [account-read-models request]
  (if-let [account ((deref account-read-models)
                    (keyword (get-in request [:params :id])))]
    (resp/response (deref account))
    (resp/not-found {})))

(defn handle-get-reserve [reserve-read-model]
   (resp/response (deref reserve-read-model)))

(defn handle-create-account [generate-id
                             get-current-time
                             event-store
                             account-write-models
                             account-read-models
                             request]
  (let [id               (keyword (or (get-in request [:params :id])
                                      (generate-id)))
        name             (get-in request [:params :name])
        [creation-result
         write-model
         read-model]     (commands/create-account event-store
                                                  id
                                                  name
                                                  (generate-id)
                                                  (get-current-time))]
    (if (= creation-result :account-created)
        (resp/created (request :uri)
                 (dosync (alter account-write-models assoc id write-model)
                         (alter account-read-models  assoc id read-model)
                         (deref write-model)))
        (resp/bad-request {}))))

(defn handle-deposit [generate-id get-current-time event-store request]
  (case
    (commands/deposit-into-account event-store
                                   (merge (request :params)
                                          {:account-id (keyword (get-in request [:params :id]))
                                           :event-id   (generate-id)
                                           :timestamp  (get-current-time)}))
    :amount-deposited (resp/response "Amount Deposited successfully")
    :deposit-rejected (resp/bad-request {})
    (resp/bad-request {})))

(defn handle-withdrawal [generate-id
                         get-current-time
                         event-store
                         account-write-models
                         request]
  (let [account-id (keyword (get-in request [:params :id]))]
    (case (commands/withdraw-from-account event-store
                                          ((deref account-write-models) account-id)
                                          (merge (request :params)
                                                 {:account-id account-id
                                                  :event-id   (generate-id)
                                                  :timestamp  (get-current-time)}))
    :amount-withdrawn (resp/response "Amount withdrawn successfully")
    :withdrawal-rejected (resp/bad-request {})
    (resp/bad-request {}))))


(defn router [{event-store          :event-store
               account-write-models :account-write-models
               account-read-models  :account-read-models
               reserve-read-model   :reserve-read-model}]
  (routes
    (GET  "/account/:id" request (handle-get-account account-read-models request))
    (GET  "/reserve/" [] (handle-get-reserve reserve-read-model))
    (POST "/account/" request (handle-create-account generate-id
                                                     get-current-time
                                                     event-store
                                                     account-write-models
                                                     account-read-models
                                                     request))
    (POST "/account/:id" request (handle-create-account generate-id
                                                        get-current-time
                                                        event-store
                                                        account-write-models
                                                        account-read-models
                                                        request))
    (POST "/account/deposit/:id" request (handle-deposit generate-id
                                                         get-current-time
                                                         event-store
                                                         request))
    (POST "/account/withdraw/:id" request (handle-withdrawal generate-id
                                                             get-current-time
                                                             event-store
                                                             account-read-models
                                                             request))
    (route/not-found "Page not found")))


(defn start-app [refs]
  (run-jetty (-> (router refs)
                 wrap-keyword-params
                 wrap->kebab->snake
                 wrap-json-params
                 wrap-json-response)
             {:port  (Integer/valueOf (or (System/getenv "PORT") "8080"))
              :join? false}))
