(ns gemini.core
  (:require
   [clojure.java.io :as io])
  (:import
   (com.omarpolo.gemini Request)))

(defmacro ^:private request->map [& args]
  `(try
     (let [req# (Request. ~@args)]
       {:request req#
        :code    (.getCode req#)
        :meta    (.getMeta req#)
        :body    (.body req#)})
     (catch Throwable e#
       {:error e#})))

(defn fetch
  "Make a gemini request.  `uri` may be a URI, URL or string, and
  represent the request to perform.  `host` and `port` are extracted
  from the given `uri` in not given, and port defaults to 1965.  The
  returned request needs to be closed when done."
  ([uri]
   (request->map uri))
  ([host uri]
   (fetch host 1965 uri))
  ([host port uri]
   (request->map host port uri)))

(defn body-as-string!
  "Read all the response into a strings and returns it.  The request
  will be closed."
  [{r :request}]
  (let [sw (java.io.StringWriter.)]
    (with-open [r r]
      (io/copy (.body r) sw)
      (.toString sw))))

(defn close
  "Close a request."
  [{r :request}]
  (.close r))

(defmacro with-request
  "Make a request, eval `body` when it succeed and automatically close
  the request, or throw an exception if the request fails."
  [[var req] & body]
  `(let [~var ~req]
     (when-let [e# (:error ~var)]
       (throw e#))
     (with-open [req# (:request ~var)]
       ~@body)))


;; helpers

(def code-description
  "Human description for every response code."
  {10 "input"
   11 "sensitive input"
   20 "success"
   30 "temporary redirect"
   31 "permanent redirect"
   40 "temporary failure"
   41 "server unavailable"
   42 "CGI error"
   43 "proxy error"
   44 "slow down"
   50 "permanent failure"
   51 "not found"
   52 "gone"
   53 "proxy request refused"
   59 "bad request"
   60 "client certificate required"
   61 "certificate not authorized"
   62 "certificate not valid"})

(defn is-input?                [{c :code}] (= 1 (/ c 10)))
(defn is-success?              [{c :code}] (= 2 (/ c 10)))
(defn is-redirect?             [{c :code}] (= 3 (/ c 10)))
(defn is-temporary-failure?    [{c :code}] (= 4 (/ c 10)))
(defn is-permanent-failure?    [{c :code}] (= 5 (/ c 10)))
(defn is-client-cert-required? [{c :code}] (= 6 (/ c 10)))
