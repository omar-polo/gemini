# Gemini library for clojure

`gemini.core` is a clojure library to make Gemini requests.


## Usage

```clojure
user=> (require '[gemini.core :as gemini])
```

#### fetch

`fetch` makes a Gemini request and returns a map with `:request`,
`:meta`, `:code` and `:body` as keys, or `:error` if an error occur.

The request needs to be closed afterwards using `close`, or calling
the `.close` method on the `:request` object.

```clojure
user=> (gemini/fetch "gemini://gemini.circumlunar.space/")
{:request
 #object[com.omarpolo.gemini.Request 0x3b270767 "com.omarpolo.gemini.Request@3b270767"],
 :meta "gemini://gemini.circumlunar.space/",
 :code 31,
 :body
 #object[java.io.BufferedReader 0x49358b66 "java.io.BufferedReader@49358b66"]}
```

#### body-as-string!

Read all the response into a string and returns it.  It also closes
the request automatically.

```clojure
user=> (-> (gemini/fetch "gemini://gemini.circumlunar.space/")
           gemini/body-as-string!)
"# Project Gemini\n\n## Overview\n\nGemini is a new internet protocol which..."
```

#### close

Closes a request.

#### with-request

Like `with-open`, but specifically for the requests:

```clojure
user=> (with-request [req (fetch "gemini://gemini.circumlunar.space/")]
         ,,,)
```
