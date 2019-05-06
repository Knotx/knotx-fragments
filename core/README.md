# Fragments Handler
This module contains logic of building a *Task* (a directed graph) consisting of *Actions*.

Fragments can contain `data-knotx-task` entry in their configuration. If it is present, then
the processing logic of defined *Task* is applied.

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
product-fallback {
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
product-fallback {
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

## Behaviours 
