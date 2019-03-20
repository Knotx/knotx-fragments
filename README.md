# Knot.x Knot Engine

# How does it works
It is a **Handler** that processes a request during HTTP Server request processing.
It operates on **Fragments** that are result of breaking request into smaller, independent parts.
Knot Engine processes Fragments using **Knots**, that are self-contained services that enriches 
and transform Fragments.
Each Fragment defines its own processing path which is called **Flow**.
A Flow is a **graph of Steps** that Fragment will be routed through by the Knot Engine. 
A **Step** is a specific use of Knot (it calls logic in Knot in specified way and with defined behaviour).

**Knot Engine processes all Fragments with defined Flow.**

# How to configure
Flow can be defined in two ways, either by `flowAlias` or `flow` configuration 
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