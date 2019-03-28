# Fragments Handler

## Action
A graph node delegates fragment processing to  
`java.util.function.Function<FragmentContext, Single<FragmentResult>>`. It represents a function 
that accepts FragmentContext as an argument and produces Single<FragmentResult> as a result.

So a graph node executes a method:
```java
Single<FragmentResult> apply(FragmentContext fragmentContext);
```
The method is RXfied version of: 
```java
void apply(FragmentContext fragmentContext, Handler<AsyncResult<FragmentResult>> resultHandler);
```

Actions implements the interface above and can:
- do some fragment logic
- add some behaviour to fragment logic.

### Example
Fragments can contain `data-knotx-task` entry in their configuration. If it is present, then
the graph processing logic is applied.

The example HTML markup:
```html
<knotx:snippet data-knotx-task="pdp">
</knotx:snippet>
```

And in the configuration we have:
```hocon
actions {
  fetch-product-with-cache {
    factory = "cache"
    config {
      payload = "products"
    }
    doAction = fetch-product-with-circuit-breaker
  }
  
  fetch-product-with-circuit-breaker {
    factory = "cb"
    config {
      circuitBreakerName = product-circuit-breaker
      circuitBreakerOptions {
        timeout = 800
        resetTimeout = 10000
        maxFailures = 3
        fallbackOnFailure = true
      }
    }
    doAction = fetch-product
  }
  
  fetch-product {
    factory = "eb"
    config {
      address = "knotx.knot.books"
      deliveryOptions {
        sendTimeout = 1000
      }
    }
  }
  
  apply-fallback {
    factory = "static"
    config {
      class = <div>Product not available at the moment</div>
    }
  }
}
```