# Fragments Supplier API
Contains the generic [Handler](https://vertx.io/docs/apidocs/io/vertx/core/Handler.html) implementation,
[FragmentsSupplierHandler](https://github.com/Knotx/knotx-fragments/blob/master/supplier/api/src/main/java/io/knotx/fragments/supplier/api/FragmentsSupplierHandler.java),
that can be re-used in Fragments Suppliers implementations.

It gets Fragments from [FragmentsSupplier](https://github.com/Knotx/knotx-fragments/blob/master/supplier/api/src/main/java/io/knotx/fragments/supplier/api/FragmentsSupplier.java), 
handles errors and finally saves fragments in the [`RoutingContext`](https://vertx.io/docs/apidocs/io/vertx/ext/web/RoutingContext.html) 
under `"fragments"` key.