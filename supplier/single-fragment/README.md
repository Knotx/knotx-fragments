# Knot.x Single Fragment Supplier Handler
This module contains a [Handler](https://vertx.io/docs/apidocs/io/vertx/core/Handler.html)
implementation that adds a single [`Fragment`](https://github.com/Knotx/knotx-fragments/tree/master/api)
to the [`RoutingContext`](https://vertx.io/docs/apidocs/io/vertx/ext/web/RoutingContext.html) under `"fragments"` key.

### How to configure
You may configure initial fragment with `type`, `body`, `configuration` adn `payload`.
Read more in the [Data Object docs](https://github.com/Knotx/knotx-fragments/blob/master/supplier/single-fragment/docs/asciidoc/dataobjects.adoc).