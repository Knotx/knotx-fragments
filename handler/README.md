# Fragments Handler
It is a [**Handler**](https://github.com/Knotx/knotx-server-http/tree/master/api#routing-handlers)
that processes Fragments during [HTTP Server request processing](https://github.com/Knotx/knotx-server-http#how-does-it-work).

## How does it work

Fragments Handler evaluates all Fragments independently using a map-reduce strategy. The `map` and 
`reduce` parts are implemented with `RX Java`, see the `flatMap` and `collect` methods. Fragment 
processing is delegated to [Fragments Engine](https://github.com/Knotx/knotx-fragments/tree/master/handler/engine)
that executes a `Task` logic (a directed graph). 

Fragments Handler reads a Task name from a Fragment configuration (it checks the `data-knotx-task` key by default)
and builds a directed Actions graph based on its [options](https://github.com/Knotx/knotx-fragments/blob/master/handler/core/src/main/java/io/knotx/fragments/handler/options/FragmentsHandlerOptions.java), 
the `tasks` node. If it is present, then the processing logic of a defined *Task* is applied (by Fragments Engine).

The diagram below depicts the map-reduce logic.

![RXfied processing diagram](core/assets/images/all_in_one_processing.png)

Task defines a directed Actions graph, those actions are configured by the `actions` configuration entry.
Each Action can:
- simple `actions` that define your business logic
- `behaviours` that define the behaviour that can "wrap" your business logic with additional features (e.g. stability patterns).

The example below says more than 1000 words. 

### Example

The example HTML markup:

```html
<knotx:snippet data-knotx-task="pdp">
</knotx:snippet>
```

And in the configuration we have:

```hocon
tasks {
  pdp {
    action = fetch-product-with-circuit-breaker
    onTransition {
      _error {
        action = product-fallback
      }
    }
  }
}

actions {
  # behaviour  
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
  # simple action
  fetch-product {
    factory = http
    config {
      endpointOptions {
        path = /product/id
        domain = webapi
        port = 8080
        allowedRequestHeaders = ["Content-Type"]
      }
    }
  }
  # simple action with fallback
  product-fallback {
    factory = "inline-body"
    config {
      body = <div>Product not available at the moment</div>
    }
  }
}
```

## Actions

### HTTP Action
This action is defined in [Knot.x Data Bridg](https://github.com/Knotx/knotx-data-bridge/tree/master/http).

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
The default `alias` is action alias.

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

```hocon
  {
    someKey {
      someNestedKey {
        attr1 = value1
        attr2 = value2 
      }
    }
  }
```

and key value `someKey.someNestedKey` body value will look like:

```hocon
  { 
    attr1 = value1
    attr2 = value2 
  }
```

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