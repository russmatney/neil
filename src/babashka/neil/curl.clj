(ns babashka.neil.curl
  {:no-doc true}
  (:require
   [babashka.curl :as curl]
   [babashka.fs :as fs]
   [cheshire.core :as cheshire]))

(import java.net.URLEncoder)

(defn url-encode [s] (URLEncoder/encode s "UTF-8"))

(def dev-github-user (System/getenv "BABASHKA_NEIL_DEV_GITHUB_USER"))
(def dev-github-token (System/getenv "BABASHKA_NEIL_DEV_GITHUB_TOKEN"))

(def curl-opts
  (merge {:throw      false
          :compressed (not (fs/windows?))}
         (when (and dev-github-user dev-github-token)
           {:basic-auth [dev-github-user dev-github-token]})))

(defn do-curl-get-json [url]
  (-> (curl/get url curl-opts)
      :body (cheshire/parse-string true)))

(def curl-cache
  "An in-memory cache reducing repeated api calls.
  Intended to support test code."
  (atom {}))

(defn clear-cache []
  (reset! curl-cache {}))

;; TODO flag via env var
(def dev-cache-enabled true)

(defn curl-get-json [url]
  (if dev-cache-enabled
    (if-let [cached (@curl-cache url)]
      cached
      (let [result (do-curl-get-json url)]
        (swap! curl-cache assoc url result)
        result))
    (do-curl-get-json url)))
