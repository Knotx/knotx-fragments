# Fragments Engine Handler

## Operation proxy
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

Operation proxies decorate operation logic (a fragment transformation) with some behaviour.

```hocon
evaluations {
  pdp {
    operation = pdp-request-scope-cache-proxy
    onTransition {
      next {
        operation = te-eb-proxy
      },
      error {
        operation = fallback-result
      }
    }
  }
}
```

```html
<knotx:snippet fragment="pdp">

</knotx:snippet>

```

```hocon
proxies {
  pdp-request-scope-cache-proxy {
    factory = "cache"
    config {
      payload = "products"
    }
    next = product-circuit-breaker-proxy
  }
  
  product-circuit-breaker-proxy {
    factory = "cb"
    config {
      circuitBreakerName = product-circuit-breaker-proxy
      circuitBreakerOptions {
        timeout = 800
        resetTimeout = 10000
        maxFailures = 3
        fallbackOnFailure = true
      }
    }
    next = product-eb-proxy
  }
  
  product-eb-proxy {
    factory = "eb"
    config {
      address = "knotx.knot.books"
      deliveryOptions {
        sendTimeout = 1000
      }
    }
  }
  
  offers-db-classpath-proxy {
    factory = "classpath"
    config {
      class = "io.knotx.postgres.OffersLogic.class"
    }
  }
  
  book-circuit-breaker-proxy {
    factory = "cb"
    config {
      circuitBreakerName = product-circuit-breaker-proxy
      circuitBreakerOptions {
        timeout = 800
        resetTimeout = 10000
        maxFailures = 3
        fallbackOnFailure = true
      }
    }
    next = book-http-proxy
  }
  
  book-http-proxy {
    factory = "http"
    config {
      url = "http://localhost:8080/api/"
    }
  }
}

```