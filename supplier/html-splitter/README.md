# Knot.x HTML Fragment Supplier Handler
This module contains a [Handler](https://vertx.io/docs/apidocs/io/vertx/core/Handler.html)
an implementation that splits a HTML request body into [`Fragments`](https://github.com/Knotx/knotx-fragments/tree/master/api) and adds them
to the [`RoutingContext`](https://vertx.io/docs/apidocs/io/vertx/ext/web/RoutingContext.html) under `"fragments"` key.

## How does it work
HTML Fragment Supplier reads a template (HTML markup) from the [Request Context](https://github.com/Knotx/knotx-server-http/blob/master/api/docs/asciidoc/dataobjects.adoc#requestcontext)
response body, then splits it with the following regexp:
```
<knotx:(?<type>\w+)(?<attributes>.*?[^>])>(?<body>.*?)</knotx:\1>
```
This regexp contains 3 matching groups:
- `type` - type of the fragment,
- `attributes` - any configuration of fragment passed in the attributes (later transferred to JsonObject with `(?<key>[\\w\\-]+)\\s*=\\s*(?<value>'((?:\\\\'|[^'])*)'|\"((?:\\\\\"|[^\"])*)\")` regexp),
- `body` - actual markup of the fragment.

Finally, the `RequestContext` response body is cleared (set to `null`) and fragments are saved
into [`RoutingContext`](https://vertx.io/docs/apidocs/io/vertx/ext/web/RoutingContext.html) under `"fragments"` key.

In case when there is no body or body is empty in the Request Context, an empty list of Fragments is supplied.

### Example
Let's assume, that the following markup is the [Request Context](https://github.com/Knotx/knotx-server-http/blob/master/api/docs/asciidoc/dataobjects.adoc#requestcontext)
response body (the [`ClientResponse`](https://github.com/Knotx/knotx-server-http/blob/master/api/docs/asciidoc/dataobjects.adoc#clientresponse) body):
```html
<html>
<head>
  <title>Test</title>
</head>
<body>
<h1>test</h1>
<knotx:fragmentType some-attribute="some-value">
  <h2>this is webservice no. 1</h2>
  <div>message - a</div>
</knotx:fragmentType>
</body>
</html>
```

That template will be split into 3 fragments:

- fragment of type `_STATIC` with `body`:
```html
<html>
<head>
  <title>Test</title>
</head>
<body>
<h1>test</h1>
```

- fragment of type `fragmentType` with `body`:
```html
  <h2>this is webservice no. 1</h2>
  <div>message - a</div>
```
and `configuration`:
```json
{
  "some-attribute": "some-value"
}
```

- fragment of type `_STATIC` with `body`:
```html
</body>
</html>
```

## How to use
Specify HTML Fragment Supplier Handler in the [Routing Operation](https://github.com/Knotx/knotx-server-http#routing-operations) 
handlers chain:
```hocon
{
  name = htmlFragmentsSupplier
}
```

### Example
See [the template processing example](https://github.com/Knotx/knotx-example-project/tree/master/template-processing) project.

