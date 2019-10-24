# Fragments Handler
It is a [**Handler**](https://github.com/Knotx/knotx-server-http/tree/master/api#routing-handlers)
that [evaluates Fragments](https://github.com/Knotx/knotx-fragments#evaluate-fragments). It is 
a standard [HTTP Server routing handler](https://github.com/Knotx/knotx-server-http/blob/master/README.md#routing-handler).

## How does it work
Fragments handler evaluates all fragments independently in a map-reduce fashion. It delegates fragment 
processing to the [Fragment Engine](https://github.com/Knotx/knotx-fragments/tree/master/handler/engine).  
The engine checks if the fragment requires processing and if it does then fragment processing starts.
modifications. Order and transitions between each of these executions is represented as a directed graph. A single graph is called a `Task`.

The diagram below depicts the map-reduce logic using [Marble Diagrams for Reactive Streams](https://medium.com/@jshvarts/read-marble-diagrams-like-a-pro-3d72934d3ef5):

![RXfied processing diagram](core/assets/images/all_in_one_processing.png)

Let's assume that Knot.x gets HTTP requests for data coming from many different sources. Dots at the 
`flatMap` diagram represents incoming requests. For simplicity assume that each request maps to a 
single fragment to process. Then each fragment is evaluated in isolation, not waiting for others. 
A fragment defines a task (graph) describing how to fetch the required data. The `map` and 
`onErrorResumeNext` diagrams represent a graph processing. When the fragment processing finishes, the modified fragment is
returned. Please note that an HTTP request can be mapped to many fragments. In such a case the `collect` diagram 
represents fragments joining.

Read more about the benefits [here](http://knotx.io/blog/configurable-integrations/).

## Task
Task decompose business logic into lightweight independent parts.  Those parts are graph nodes, 
connected by transitions. Graph node can, for example, represent some REST API invocation. So a task 
is a directed graph of nodes.
```
(A) ───> (B) ───> (C)
    └──> (D)
```

Tasks are configured with HOCON configuration in form of a dictionary (`taskName -> definition`):
```hocon
tasks {
  # unique task name
  myTask { // task options
    ...
  }
}
```

Let's see how tasks are instantiated. A task specifies its provider factory (with options) and its graph logic:
```hocon
tasks {
  # unique task name
  myTask {
    # factory options
    factory = configuration
    config {
      someKey = someValue
    }
    # graph logic
    graph {
      # GRAPH
    }
  }
}
```

In most cases the default task provider (`configuration`) is used, so the definition can be simplified to:
```hocon
tasks {
  # unique task name
  myTask {
    # GRAPH
  }
}
```
> Note:
> Custom tasks providers can be easily added with custom [factories](https://github.com/Knotx/knotx-fragments/blob/master/handler/core/src/main/java/io/knotx/fragments/task/TaskProviderFactory.java) 
> that register in [Task Factory](https://github.com/Knotx/knotx-fragments/blob/master/handler/core/src/main/java/io/knotx/fragments/task/TaskFactory.java).

#### Graph
As already mentioned, the task logic is defined in the form of a directed graph. Moreover, this graph is acyclic and each of its nodes can be reached only from exactly one path (transition). These properties enables us to treat the Task as a tree structure. Each graph node sets 
fragment logic to perform and outgoing edges, called Transitions. Its configuration looks like:
```hocon
graph {
  # node logic options
  node { }
  # node outgoing edges
  onTransitions {
    _success { }
    _error { }
    someCustomTransition { }
  }
}
```
There are two sections:
- `node` defines a fragment processing logic
- `onTransitions` is a map that represents outgoing edges in a graph

#### Node processing
The node responsibility can be described as: 
> Graph node gets a fragment, processes it and responds with Transition. 
So a node is the function `F -> (F', T)` where `F` is the Fragment, `F'` is a modified Fragment and `T` is the Transition.

The node definition is abstract. It allows to define simple processing nodes but also more complex 
structures such as a list of subgraphs. Furthermore, such a definition inspires to provide custom 
node implementations.

The `node` configuration is simple:
```hocon
graph {
  node {
    factory = action
    config {
      # some unique action name
      action = book-rest-api
    }
  }
}


```
The `factory` parameter specifies a node factory name, `config` contains all options passed to 
the factory. 

Knot.x provides two node implementations:
- **Action node** that represents simple steps in a graph such as integration with a data source
- **Subtasks node** that is a list of unnamed tasks (subtasks) that are evaluated in parallel

##### Action node
An *action node* declares an [action](#actions) to execute by its name:
```hocon
graph {
  node {
    factory = action
    config {
      action = book-rest-api
    }
  }
}
```
The above example specifies the action node that delegates processing to the `book-rest-api` action.

Knot.x allows simplifying action nodes declaration:
```hocon
action = book-rest-api
```

So the task definition is:
```hocon
tasks {
  myTask {
    action = book-rest-api
  }  
}
```
It is the `myTask` task with the action node using the `book-rest-api` action and no transitions.

A nice syntax sugar!

##### Subtasks node
SubTasks node is a node containing a list of sub-tasks. It evaluates all of them sequentially. However, 
all the operations are non-blocking, so it doesn't wait for previous subtasks to finish. Because of that, they are effectively executed in parallel

Moreover, a list of sub-tasks must fit the `F -> (F',T)` function. Each subtask has its fragment 
context, execute it's logic and update the fragment's payload (its own copy). Finally, when all sub-tasks are completed, 
all payloads are merged and the new Fragment is returned.

> Note that body modifications are not allowed because of the parallel execution of subtask nodes and the final `body` form cannot be determined. However, updating the fragment's `payload` is fine since all subtask nodes have their unique namespaces.

A subtasks node configuration looks like:
```hocon
graph {
  node {
    factory = subTasks
    config {
      subTasks = [
        { 
          action = book-rest-api 
        },
        { 
          action = author-rest-api 
        }
      ]
    }
  }
}
```

It follows the same simplification rules as action nodes:
```hocon
subTasks = [
  { 
    action = book-rest-api 
  },
  { 
    action = author-rest-api 
  }
]
```

Please note that it is a list of subtasks, not action nodes! In the example above, the `book-rest-api`  
and `author-rest-api` actions are executed in parallel as two independent tasks (graphs) with one node (action). 

See the [example section](#complex-example) for a more complex scenario. Before we see the full power of 
graphs, we need to understand how nodes are connected.

##### Transitions
A directed graph consists of nodes and edges. Edges are called transitions. Their configuration looks like:
```hocon
onTransitions {
  _success {
    # next node when _success transition
  }
  _error {
    # next node when error occurs
  }
  customTransition {
    # next node when custom transition
  }
}
```
Transition is a simple text. The `_success` and `_error` transitions are the default ones. However, 
they are not mandatory!

There are two important rules to remember:
> If a node responds with *_success* transition, but the transition is not configured, then processing is finished.

> If a node responds with *_error* transition, but the transition is not configured, then an exception is returned.

Nodes can declare custom transitions. Custom transitions allow to react to non standard situations 
such as data sources timeouts, fallbacks etc.

#### Complex example
```hocon
tasks {
  book-and-author-task {
    subTasks = [
      { 
        action = book-rest-api # subtask
        onTransitions {
          _success {
            action = score-api
            onTransitions {
              noScore { # custom transition
                action = score-estimation
                # _success {} - subtask finished
              }
              # _success {} - subtask finished
            }   
          }
          _error {
            action = book-from-cache
          } 
        }
      },
      {
        action = author-rest-api # subtask
        # _success {} - subtask finished
      }
    ]
    onTransitions {
      _error {
        action = book-and-author-fallback
      }
    }
  # END subtasks
  }
}
```
The above task fetches book and author details in parallel. The `book-rest-api` action node defines 
`_success` and `_error` transitions. If any subtask fail then the `book-and-author-fallback` node action 
is executed. 

## Actions
Action defines action node logic, it is the `F -> (F',T)` function. Actions integrate with external data sources, 
do some fragments modifications or fetch data. A data source response is saved in a Fragment's payload (JSON object) 
under an Action's name key and a "\_result" sub-key:
```json
{
  "book": {
    "_result": { }
  }
}
```

Actions are divided in two types:
- `simple actions` that actually modify the fragment (e.g. integrate with data sources and saves the payload)
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
Inline Payload Action puts JSON / JSON Array in Fragment payload with a specified key (alias). Its 
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
If no key is specified the whole payload will be copied. A key can direct nested values. For example 
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
The `doAction` attribute specifies a wrapped simple action by its name. When `doAction` throws an error 
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
