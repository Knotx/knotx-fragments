# Knot.x Knot Engine

# How does it works
It is a handler in server flow...

# How to configure
Knot Engine processes **all** fragments containing `flowAlias` or `flow` configuration 
entries. This section describes how flow can be defined in fragment configuration. The last chapter
explains how Knot Engine can be configured in [Knot.x Server](https://github.com/Knotx/knotx-server-http).

## Flow configuration
Knot Engine allows to pre-configure flows and it steps.

### Pre-configured flows
The fragment configuration looks like
```json
{
  "flowAlias": "default"
}
```
and the handler configuration contains
```hocon
flows {
  default {
    stepAlias = databridge
    onTranstion {
      next {
        stepAlias = te
      }
    }
  }
}
steps {
  databridge {
    address = knotx.knot.databridge
    # delivery options, circuit breaker options, knot params
  }
  te {
    address = knotx.knot.te
    # delivery options, circuit breaker options, knot params
  }
}

```
### Flow with pre-configured steps
The fragment configuration looks like
```json
{
  "flow": {
    "stepAlias": "databridge",
    "onTransition": {
      "next": {
        "stepAlias": "te"
      }
    }
  }
}
```
and the handler configuration contains
```hocon
steps {
  databridge {
    address = knotx.knot.databridge
    # delivery options, circuit breaker options, knot params
  }
  te {
    address = knotx.knot.te
    # delivery options, circuit breaker options, knot params
  }
}
```
Steps configurations are then merged based on alias.

The previous example can be shortened to
```json
{
  "flow": "databridge,te"
}
```
This short option does not allow to define other transitions than `next`.

### Flow fully embedded in fragment configuration
The fragment configuration looks like
```json
{
  "flow": {
    "step": {
      "address": "knotx.knot.databridge",
      "deliveryOptions": {},
      "circuitBreakerOptions": {},
      "knotParams": {}
    },
    "onTransition": {
      "next": {
        "step": {
          "address": "knotx.knot.te",
          "deliveryOptions": {},
          "circuitBreakerOptions": {}
        }
      }
    }
  }
}
```

### Merging options
The fragment configuration looks like
```html
{
  "flow": {
    "stepAlias": "databridge",
    "step": {
      "address": "knotx.databrige.overriden.address"
    }
    "onTransition": {
      "next": {
        "stepAlias": "te"
      }
    }
  }
}
```
and the handler configuration contains
```hocon
steps {
  databridge {
    address = knotx.knot.databridge
    # delivery options, circuit breaker options, knot params
  }
  te {
    address = knotx.knot.te
    # delivery options, circuit breaker options, knot params
  }
}
```


## Handler configuration

```hocon
{
  name = knotEngineHandler
  config {
    # preconfigred flows
    flows {
      default {
        stepAlias = databridge
        onTranstion {
          next {
            stepAlias = te
          }
        }
      }
    }
    # those steps 
    steps {
      databridge-books {
        address = knotx.knot.databridge
        # deliveryOptions {
        #   sendTimeout = 1000
        #   localOnly = false
        # }
        config {
          # knot params
        }
      }
      te {
        address = knotx.knot.te
        # deliveryOptions {
        #   sendTimeout = 1000
        #   localOnly = false
        # }
      }
    }
  }
}
```