# Fragments Handler
It is a [**Handler**](https://github.com/Knotx/knotx-server-http/tree/master/api#routing-handlers)
that processes Fragments during [HTTP Server request processing](https://github.com/Knotx/knotx-server-http#how-does-it-work).

## How does it work

Fragments Handler evaluates all Fragments independently using a **map-reduce** strategy. Each Fragment 
is processed by [Fragments Engine](https://github.com/Knotx/knotx-fragments/tree/master/handler/engine)
that evaluates a `Task` logic (a directed graph).

Fragments Handler reads a Task name from Fragment configuration (it checks the `data-knotx-task` key by default)
and builds a directed graph of nodes. Task node can be [Action](#actions) or list of sub-graphs (sub-tasks). 

The diagram below depicts the map-reduce logic with RX Java marble diagrams:

![RXfied processing diagram](core/assets/images/all_in_one_processing.png)

It represents a page containing many Fragments, each Fragment is evaluated, when an error occurs, some 
fallback can be configured. When all Fragments are processed, then they are merged and returned as 
a page body.

Moving from macro to micro perspective, when Fragments are result of HTML splitting, the example can 
look like:
```html
<knotx:snippet data-knotx-task="pdp">
{{get-product._result.data}}
</knotx:snippet>
```

And then in the configuration we have:

```hocon
# tasks map (task name -> graph of nodes)
tasks {
  # task name
  pdp {
    # task definition
    action = get-product
    onTransitions {
      _success {
        action = fill-placeholders
      } 
      _error {
        action = handlebars
      }
    }
  }
}

actions {
  get-product { ... }
  handlebars { ... }
  apply-fallback {
    factory = "inline-body"
    config {
      body = <div>Product not available at the moment</div>
    }
  }
}
```
See next chapters to understand what are Tasks in details.


## Task
Task defines a directed graph of nodes. Node gets Fragment and responds with modified Fragment and Transition. 
So node contract is a function:

```
F -> (F', T)
```

where `F` is Fragment, `F'` is the new Fragment, `T` is Transition.

Such definition encourages to provide custom nodes. Projects can easily extend the default node implementations:
**Action node** (then called **Action**) that integrates with a data source and enables applying stability patterns in a declarative way
**SubTask node** that is a list of unnamed tasks (sub-tasks) that are evaluated in parallel

The most simple Task definition can be:
```hocon
# action name
action = a
```

It is the alias to the Action node with the `a` action name and no Transitions.

This alias is translated to:
```hocon
# node configuration
node {
  # Action node factory name
  factory = action
  config {
    action = a
  }
}
```

Task usually defines three parts:
```hocon
# task factory
factory {}
# node configuration
node{}
# node edges
onTransitions {}
```

So in fact our simple example is translated to:
```hocon
# task factory
factory {
  name = default
}
# node configuration
node {
  # Action node factory name
  factory = action
  config {
    action = a
  }
}
# node edges
onTransitions {}
```

With SubTasks node parallel sub-tasks (sub-graphs) are easy to configure.

Let's see more complex example:
```hocon
subTasks = [
  { 
    action = book
    onTransitions {
      _success {
        action = score
      }
      _error {
        action = book-from-cache
      } 
    }
  },
  { action = author }
]
onTransitions {
  _error {
    action = book-and-author-fallback
  }
}
```
There is a Task that fetches book and author details in parallel. However, the `book` Action node defines 
`_success` and `_error` transitions. So the `subTasks` node schedules a list of sub-tasks to execute 
and wait until all of them complete. Additionally it defines the `error` Transition if any of those
sub-tasks will fail.

## Actions
Action is a node. So it is a function `F -> (F',T)`. Actions integrate with external data sources, 
do some updates or fetch data. A data source response is saved in a Fragment's payload (JSON object) 
under an Action's name key and a "\_result" sub-key:
```json
{
  "book": {
    "_result": { ... }
  }
}
```

Actions are divided in two types:
- `simple actions` that integrate with data sources
- `behaviours` that wrap simple actions and add some "behaviour"

### Simple Actions

#### HTTP Action
The HTTP Action fetches JSON data from REST APIs (GET request). See more [here](https://github.com/Knotx/knotx-data-bridge/tree/master/http).

#### Inline Body Action
Inline Body Action replaces Fragment body with specified one. Its configuration looks like:
```hocon
factory = "inline-body"
config {
  body = <div>Product not available at the moment</div>
}

```
The default `body` value is empty content.

#### Inline Payload Action
Inline Payload Action puts JSON / JSON Array in Fragment payload with specified key (alias). Its 
configuration looks like:
```hocon
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
```
The default `alias` is action alias.

#### Payload To Body Action
Payload To Body Action copies to Fragment body specified payload key value. Its configuration looks like:
```hocon
factory = payload-to-body
config {
  key = "some payload key"
}
```
If no key specified whole payload will be copied. A key can direct nested values. For example 
for the payload:
```hocon
someKey {
  someNestedKey {
    attr1 = value1
    attr2 = value2 
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

### Behaviours

#### Circuit Breaker Behaviour
It wraps a simple action with the [Circuit Breaker implementation from Vert.x](https://vertx.io/docs/vertx-circuit-breaker/java/).
Its configuration looks like:
```hocon
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
```
The `doAction` attribute specifies a wrapped simple action by its name. When `doAction` throws error 
or times out then the custom `fallback` transition is returned.

#### In-memory Cache Behaviour
It wraps a simple action with cache. It caches a payload values added by a `doAction` action and 
puts cached values in next invocations. It uses in-memory Guava cache implementation. The 
configuration looks like:
```hocon
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
```
Please note that cacheKey can be parametrized with request data like params, headers etc. Read 
[Knot.x HTTP Server Common Placeholders](https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders)
documentation for more details.

## SubTasks node
Subtask is a node containing a list of sub-tasks. It evaluates all of them sequentially. However, 
all operations are non-blocking so they are executed in parallel. 

Moreover, a list of sub-tasks must fit the `F -> (F',T)` function. Each subtask has its Fragment 
context, execute it's logic and update Fragment's payload. Finally, when all sub-tasks are completed, 
all payloads are merged and the new Fragment is returned.

> Note that body modifications are not allowed.

The most simple configuration looks like:
```hocon
subTasks = [
  {
    action = a
  },
  {
    action = b
  }
]
```

It is translated to:
```hocon
node {
  factory = subTasks
  config {
    subTasks = [
      {
        node {
          factory = action
          config {
            action = a
          } 
        }
      }
      {
        ...
      }
    ]
  }
}
```

