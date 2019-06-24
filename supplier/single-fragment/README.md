# Knot.x Single Fragment Supplier Handler
This module contains a [Handler](https://vertx.io/docs/apidocs/io/vertx/core/Handler.html)
implementation that adds a single [`Fragment`](https://github.com/Knotx/knotx-fragments/tree/master/api)
to the [`RoutingContext`](https://vertx.io/docs/apidocs/io/vertx/ext/web/RoutingContext.html) under `"fragments"` key.

### How to configure
You may configure initial fragment with `type`, `body`, `configuration` adn `payload`.
Read more in the [Data Object docs](https://github.com/Knotx/knotx-fragments/blob/master/supplier/single-fragment/docs/asciidoc/dataobjects.adoc).

## How to use
Simply add a [Routing Operation](https://github.com/Knotx/knotx-server-http#routing-operations)
entry:

```hocon
{
  name = singleFragmentSupplier
  config {
    type = json
    configuration {
      data-knotx-task = my-task
    }
  }
}
```

You may find more about what is available in the `config` [here](https://github.com/Knotx/knotx-fragments/blob/master/supplier/single-fragment/docs/asciidoc/dataobjects.adoc). 