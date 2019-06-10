# Fragments Handler
This module contains the logic of building a *Task* (a directed graph) consisting of *Actions*.

Fragments can contain a `data-knotx-task` entry in their configuration. If it is present, then
the processing logic of a defined *Task* is applied.

The example HTML markup:
```html
<knotx:snippet data-knotx-task="pdp">
</knotx:snippet>
```

And in the configuration we have:
```hocon
tasks {
  pdp {
    action = fetch-product-with-cache
    onTransition {
      _error {
        action = product-fallback
      }
    }
  }
}

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
  
  product-fallback {
    factory = "inline-body"
    config {
      body = <div>Product not available at the moment</div>
    }
  }
}
```

Read more about configuring fragment graph in the [Data Object docs](https://github.com/Knotx/knotx-fragments-handler/blob/master/core/docs/asciidoc/dataobjects.adoc).


## Actions

### Inline Body Action
Inline Body Action replaces Fragment body with specified body. Its configuration looks like:
```hocon
product-body-fallback {
  factory = "inline-body"
  config {
    body = <div>Product not available at the moment</div>
  }
}
```

The default `body` value is empty content.

### Inline Payload Action
Inline Payload Action puts JSON / JSON Array in Fragment payload with specified key (alias). Its 
configuration looks like:
```hocon
product-payload-fallback {
  factory = "inline-payload"
  config {
    alias = product
    payload {
      productId = 1234
      description = "some description"
    }
    # payload = [
    #   "first product", "second product"
    # ]
  }
}
```

### Payload To Body Action
Payload To Body Action copies to Fragment body specified payload key value. Its configuration looks like:

```hocon
  copyToBody {
    factory = payload-to-body
    config {
      key = "smoe payload key"
    }
  }
```
If no key specified whole payload will be copied
Key can direct nested values. For example for payload;

```json
  {
    someKey: {
      someNestedKey: {
        attr1: value1,
        attr2: value2, 
      }
    }
  }
```

and key value `someKey.someNestedKey` body value will look like:

```json
  { 
    attr1: value1,
    attr2: value2, 
  }
    
## Behaviours 

### Circuit Breaker Action
Circuit Breaker Action uses the [Circuit Breaker pattern from Vert.x](https://vertx.io/docs/vertx-circuit-breaker/java/).
It implements the solution with a fallback strategy. When doAction throws error or times out then the
custom `fallback` transition is returned.

The configuration looks like:
```hocon
product-cb {
  factory = "cb"
  config {
    circuitBreakerName = product-cb-name
    circuitBreakerOptions {
      # number of failure before opening the circuit
      maxFailures = 3
      # consider a failure if the operation does not succeed in time
      timeout = 2000
      # time spent in open state before attempting to re-try
      resetTimeout = 10000
    }
  }
  doAction = product
}
```

### In-memory Cache Action
In-memory Cache Action caches doAction payload result and puts cached values in next invocations. It 
uses in-memory Guava cache implementation. The configuration looks like:
```hocon
product-cache {
  factory = "in-memory-cache"
  config {
    cache {
      maximumSize = 1000
      # in milliseconds
      ttl = 5000
    }
    cacheKey = "product-{param.id}"
    payloadKey = product
  }
  doAction = product-cb
}
```
Please note that cacheKey can be parametrized with request data like params, headers etc. Read 
[Knot.x HTTP Server Common Placeholders](https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders)
documentation for more details. 
