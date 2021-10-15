(ns gemini.core
  (:require
   [clojure.java.io :as io])
  (:import
   (java.net URI)
   (com.omarpolo.gemini Request)))

(comment
  (set! *warn-on-reflection* true)
)


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

(defn is-input?                [{c :code}] (= 1 (quot c 10)))
(defn is-success?              [{c :code}] (= 2 (quot c 10)))
(defn is-redirect?             [{c :code}] (= 3 (quot c 10)))
(defn is-temporary-failure?    [{c :code}] (= 4 (quot c 10)))
(defn is-permanent-failure?    [{c :code}] (= 5 (quot c 10)))
(defn is-client-cert-required? [{c :code}] (= 6 (quot c 10)))



(defn- parse-params [{:keys [proxy request follow-redirects?]}]
  (when (and (:host proxy)
             (not (:port proxy)))
    (throw (ex-info "invalid proxy definition" {:got    proxy
                                                :reason "missing proxy host"})))
  {:host              (:host proxy)
   :port              (:port proxy)
   :request           (or request
                          (throw (ex-info ":request is nil" {})))
   :follow-redirects? (case follow-redirects?
                        nil   0
                        false 0
                        true  5
                        follow-redirects?)})

(defn- resolve-uri [request meta]
  (let [uri (URI. request)
        rel (URI. meta)]
    (str (.resolve uri rel))))

(defn- fetch' [host port uri]
  (try
    (let [req (cond
                host  (Request. ^String host ^int port ^String uri)
                :else (Request. ^String uri))]
      {:uri     uri
       :request req
       :code    (.getCode req)
       :meta    (.getMeta req)
       :body    (.body req)})
    (catch Throwable e
      {:error e})))

(defn fetch
  "Make a gemini request.  `params` is a map with the following
  keys (only `:request` is mandatory):

  - `:proxy`: a map of `:host` and `:port`, identifies the server to
  send the requests to.  This allows to use a gemini server as a
  proxy, it doesn't do any other kind of proxying (e.g. SOCK5.)

  - `:request` the URI (as string) to require.

  - `:follow-redirects?` if `false` or `nil` don't follow redirects,
  if `true` follow up to 5 redirects, or the number of redirects to
  follow.

  Return a map with `:request`, `:code`, `:meta`, `:body` on success
  or `:error` on failure.  The request needs to be closed when done
  usign `close`."
  [params]
  (let [{:keys [host port request follow-redirects?] :as orig} (parse-params params)]
    (loop [n           follow-redirects?
           request     request
           redirected? false]
      (let [res       (fetch' host port request)
            redirect? (and (not (:error res))
                           (is-redirect? res))]
        (cond
          (:error res)            res
          (= follow-redirects? 0) res
          (and (= 0 n)
               redirect?)         (do (.close ^Request (:request res))
                                      (throw (ex-info "too many redirects"
                                                      {:original  orig
                                                       :redirects follow-redirects?
                                                       :code      (:code res)
                                                       :meta      (:meta res)})))
          redirect?               (do (.close ^Request (:request res))
                                      (recur (dec n)
                                             (resolve-uri request (:meta res))
                                             true))
          :else                   res)))))

(defn body-as-string!
  "Read all the response into a strings and returns it.  The request
  will be closed."
  [{r :request}]
  (let [sw (java.io.StringWriter.)]
    (with-open [r ^Request r]
      (io/copy (.body r) sw)
      (.toString sw))))

(defn close
  "Close a request."
  [{r :request}]
  (.close ^Request r))

(defmacro with-request
  "Make a request, eval `body` when it succeed and automatically close
  the request, or throw an exception if the request fails."
  [[var req] & body]
  `(let [~var ~req]
     (when-let [e# (:error ~var)]
       (throw e#))
     (with-open [req# (:request ~var)]
       ~@body)))
