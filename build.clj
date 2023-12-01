(ns build
    (:require [clojure.tools.build.api :as b]))


(def lib 'jamie-rodriguez/four-clojure)
(def src-dirs ["src"])
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-standalone.jar" (name lib)))


(defn clean [_]
    (b/delete {:path "target"}))

(defn uber [_]
    (clean nil)
    (b/copy-dir {:src-dirs src-dirs
                 :target-dir class-dir})
    (b/compile-clj {:basis basis
                    :src-dirs src-dirs
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis basis
             :main 'main}))
