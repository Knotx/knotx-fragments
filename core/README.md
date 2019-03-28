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

task

```hocon
tasks {
  pdp {
    action = fetch-product-with-cache
    onTransition {
      next {
        action = te
      },
      fallback {
        action = apply-fallback
      }
    }
  }
}
```

```html
<knotx:snippet knotx-task="pdp">

</knotx:snippet>

```

```hocon

decorators {
  cache {
    factory = "cache"
    config {
      payload = "products"
    }
  },
  circuit-breaker {
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
  }
}

actions {
  fetch-product {
    factory = "eb"
    config {
      address = "knotx.knot.books"
      deliveryOptions {
        sendTimeout = 1000
      }
    }
    with {
      name = cache
      with {
        name = circuit-breaker
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