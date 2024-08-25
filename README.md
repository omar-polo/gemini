# gemini.core

[![Clojars Project](https://img.shields.io/clojars/v/com.omarpolo/gemini.svg)](https://clojars.org/com.omarpolo/gemini)

A Clojure library to make Gemini requests that exposes some low-level
API to handle network requests.


## Usage

Import the library for e.g. with:

```clojure
user=> (require '[gemini.core :as gemini])
```

### Documentation

`fetch` makes a Gemini request.  The request needs to be closed
afterwards using `close`.

Takes a map with the following keys (only `:request` is mandatory):

 - `:proxy`: a map of `:host` and `:port`, identifies the server to
   send the requests to.  This allows to use a gemini server as a
   proxy, it doesn't do any other kind of proxying (e.g. SOCKS.)
 - `:request` the URI (as string) to require.
 - `:follow-redirects?` if `false` or `nil` don't follow redirects, if
   `true` follow up to 5 redirects, or the number of redirects to
   follow.

Returns a map with `:error` key if an error occur or with the
following fields if it succeeds:

 - `:uri`: the URI of the request.  May be different from the
   requested one if `:follow-redirects?` was specified.
 - `:request`: the object backing the request.
 - `:code` and `:meta` are the parsed header response.
 - `:body` an instance of a `BufferedReader`.  Note: closing the body
   is not enough, always call `close` on the returned map.
 - `:redirected?` true if a redirect was followed.

```clojure
user=> (gemini/fetch {:request "gemini://geminiprotocol.net/"
                      :follow-redirects? true})
{:uri "gemini://geminiprotocol.net/",
 :request
 #object[com.omarpolo.gemini.Request 0x6fa9ec6f "com.omarpolo.gemini.Request@6fa9ec6f"],
 :code 20,
 :meta "text/gemini",
 :body
 #object[java.io.BufferedReader 0x18a8d9e0 "java.io.BufferedReader@18a8d9e0"],
 :redirected? true}
```

`body-as-string!` reads all the response into a string and returns it.
It also closes the request automatically.

```clojure
user=> (-> {:request "gemini://geminiprotocol.net/"}
           gemini/fetch
           gemini/body-as-string!)
"# Project Gemini\n\n## Gemini in 100 words\n\nGemini is a new internet..."
```

`close` closes a request.  It needs to be called after every request.

```clojure
user=> (let [req (gemini/fetch {,,,})]
         (when-not (:error req)
           ;; do something with req
           ,,,
           (gemini/close req)))
```

`with-request` is a macro like `with-open` to handle connections easily.
It automatically closes the request and evaluates the body only when the
request is successful, otherwise throws an exception.

```clojure
user=> (gemini/with-request [req {:request "gemini://geminiprotocol.net/"}]
         ,,,)
```


## Streaming content

The `:body` keyword in the returned map is an instance of a Java
BufferedReader, so streaming content is easy.

However, `body-as-string!` needs to materialise the full reply, so in
case of an illimitate request it may never return!


## text/gemini

This library only implements the network part of Gemini, it doesn't
try to handle any kind of content.  To handle text/gemini you can use
for e.g. my [gemtext][gemtext] library:

```clojure
user=> (require '[gemtext.core :as gemtext])
nil
user=> (gemini/with-request [req {:request "gemini://geminiprotocol.net/"}]
         (gemtext/parse (:body req)))
[[:header-1 "Project Gemini"]
 [:text ""]
 [:header-2 "Gemini in 100 words"]
 [:text ""]
 [:text "Gemini is a new internet technology supporting an electronic ..."]
 ,,,]
```

The [gemtext][gemtext] library supports streaming via the
`gemtext.core/parse` transducer:

```clojure
user=> (gemini/with-request [req {:request "gemini://geminiprotocol.net/"}]
         (transduce gemtext/parser conj [] (line-seq (:body req))))
,,,
```


[gemtext]: https://github.com/omar-polo/gemtext


## License

Copyright © 2021 Omar Polo, all rights reserved.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
