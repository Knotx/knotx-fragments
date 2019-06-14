# Knot.x Splitter Handler
This module contains [Handler](https://vertx.io/docs/apidocs/io/vertx/core/Handler.html)
implementation that splits a Template into [`Fragments`](https://github.com/Knotx/knotx-fragment-api).

Template is a document that will be later processed by Knot.x instance. Current implementation (`HtmlFragmentSplitter`)
supports splitting HTML markup, but Template could be any document like PDF file, Office file or even an image.

## How does it work?
HTML Fragment Splitter reads the template (a document) from the [Request Context](https://github.com/Knotx/knotx-server-http/blob/master/api/docs/asciidoc/dataobjects.adoc#requestcontext)
body. Then it splits the HTML markup with following regexp:
```
<knotx:(?<type>\w+)(?<attributes>.*?[^>])>(?<body>.*?)</knotx:\1>
```
This regexp contains 3 matching groups:
- `type` - type of the fragment,
- `attributes` - any configuration of fragment passed in the attributes (later transferred to JsonObject with `(?<key>[\\w\\-]+)\\s*=\\s*(?<value>'((?:\\\\'|[^'])*)'|\"((?:\\\\\"|[^\"])*)\")` regexp),
- `body` - actual markup of the fragment.

Finally, `RequestContext` body is cleared (set to `null`) and fragments are saved
into [`RequestEvent`](https://github.com/Knotx/knotx-server-http/blob/master/api/docs/asciidoc/dataobjects.adoc#requestevent) fragments list.

### How Template is splitted?
Lets explain the process of the Template splitting using an example.

Let's assume, that following markup is [`ClientRequest`](https://github.com/Knotx/knotx-server-http/blob/master/api/docs/asciidoc/dataobjects.adoc#clientrequest)
body:
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

That template will be splitted into 3 fragments:

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