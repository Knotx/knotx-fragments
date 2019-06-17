# Fragments Supplier API

## Fragments Supplier
Contains a generic [Handler](https://vertx.io/docs/apidocs/io/vertx/core/Handler.html) that
can be re-used in Fragments Providers implementations.

It saves fragments in the [`RoutingContext`](https://vertx.io/docs/apidocs/io/vertx/ext/web/RoutingContext.html) 
under `"fragments"` key.