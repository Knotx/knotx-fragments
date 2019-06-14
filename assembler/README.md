# Knot.x Fragments Assembler
This module contains [Handler](https://vertx.io/docs/apidocs/io/vertx/core/Handler.html)
that joins all [`Fragments`](https://github.com/Knotx/knotx-fragment-api) and
saves the result into the [`ClientResponse`](https://github.com/Knotx/knotx-server-http/blob/master/api/docs/asciidoc/dataobjects.adoc#clientresponse) body.

## How does it work?
Fragment Assembler reads Fragments from the [`RoutingContext`](https://vertx.io/docs/apidocs/io/vertx/ext/web/RoutingContext.html) 
under `"fragments"` key and joins them all into one string, saving as the Client Response `body`.

### How Fragments are being joined?
Lets explain the process of fragments joining fragments using an example.

Fragment Assembler reads [`ClientRequest`](https://github.com/Knotx/knotx-server-http/blob/master/api/docs/asciidoc/dataobjects.adoc#clientrequest)
that contains three Fragments:
```html
<html>
<head>
  <title>Test</title>
</head>
<body>
<h1>test</h1>
```
```html
  <h2>this is webservice no. 1</h2>
  <div>message - a</div>
```
```html
</body>
</html>
```
Fragment Assembler joins all those Fragments into one string:
```html
<html>
<head>
  <title>Test</title>
</head>
<body>
<h1>test</h1>
  <h2>this is webservice no. 1</h2>
  <div>message - a</div>
</body>
</html>
```