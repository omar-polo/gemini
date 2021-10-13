# gemini.core

A Clojure library to make Gemini requests.


## Usage

Import the library for e.g. with:

```clojure
user=> (require '[gemini.core :as gemini])
```

### Documentation

`fetch` makes a Gemini request and returns a map with `:request`,
`:meta`, `:code` and `:body` as keys, or `:error` if an error occur.

The request needs to be closed afterwards using `close`.

```clojure
user=> (gemini/fetch "gemini://gemini.circumlunar.space/")
{:request
 #object[com.omarpolo.gemini.Request 0x3b270767 "com.omarpolo.gemini.Request@3b270767"],
 :meta "gemini://gemini.circumlunar.space/",
 :code 31,
 :body
 #object[java.io.BufferedReader 0x49358b66 "java.io.BufferedReader@49358b66"]}
```

`body-as-string!` reads all the response into a string and returns it.
It also closes the request automatically.

```clojure
user=> (-> (gemini/fetch "gemini://gemini.circumlunar.space/")
           gemini/body-as-string!)
"# Project Gemini\n\n## Overview\n\nGemini is a new internet protocol which..."
```

`close` closes a request.  It needs to be called after every
(successful) request.

```clojure
user=> (let [req (gemini/fetch "...")]
         (when-not (:error req)
		   ;; do something with req
		   ,,,
		   (gemini/close req)))
```

`with-request` is a macro like `with-open` to making connection
easily.  It automatically closes the request and evaluates the body
only when the request is successful, otherwise throws an exception.

```clojure
user=> (with-request [req (gemini/fetch "gemini://gemini.circumlunar.space/")]
         ,,,)
```


## Streaming content

The `:body` keyword in the returned map is an instance of a Java
BufferedReader, so streaming content is easy.

However, `body-as-string!` needs to materialise the full reply, so in
case of a streaming request it will never return!


## text/gemini

This library only implements the network part of Gemini, it doesn't
try to handle any kind of content.  To handle text/gemini you can use
e.g. the [gemtext][gemtext] library:

```clojure
user=> (require '[gemtext.core :as gemtext])
nil
user=> (gemini/with-request [req (gemini/fetch "gemini://gemini.circumlunar.space/")]
         (gemtext/parse (:body req)))
[[:header-1 "Project Gemini"]
 [:text ""]
 [:header-2 "Overview"]
 [:text ""]
 [:text "Gemini is a new internet protocol which:"]
 ,,,]
```


[gemtext]: https://github.com/omar-polo/gemtext
