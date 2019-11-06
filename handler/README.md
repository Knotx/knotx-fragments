# Fragments Handler
It is a [**Handler**](https://github.com/Knotx/knotx-server-http/tree/master/api#routing-handlers)
that [evaluates Fragments](https://github.com/Knotx/knotx-fragments#evaluate-fragments). It is 
a standard [HTTP Server routing handler](https://github.com/Knotx/knotx-server-http/blob/master/README.md#routing-handler).

## How does it work
Fragments handler evaluates all fragments independently in a map-reduce fashion. It delegates fragment 
processing to the [Fragment Engine](https://github.com/Knotx/knotx-fragments/tree/master/handler/engine).  
The engine checks if the fragment requires processing and if it does then fragment processing starts.
modifications. Order and transitions between each of these executions is represented as a directed graph. 
A single graph is called a `Task`.

The diagram below depicts the map-reduce logic using [Marble Diagrams for Reactive Streams](https://medium.com/@jshvarts/read-marble-diagrams-like-a-pro-3d72934d3ef5):

![RXfied processing diagram](core/assets/images/all_in_one_processing.png)

Let's assume that Knot.x gets HTTP requests for data coming from many different sources. Dots at the 
`flatMap` diagram represents incoming requests. For simplicity assume that each request maps to a 
single fragment to process. Then each fragment is evaluated in isolation, not waiting for others. 
A fragment defines a task (graph) describing how to fetch the required data. The `map` and 
`onErrorResumeNext` diagrams represent a graph processing. When the fragment processing finishes, the 
modified fragment is returned. Please note that an HTTP request can be mapped to many fragments. In 
such a case the `collect` diagram represents fragments joining.

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
  myTask { 
    # task provider options and graph logic
  }
}
```

Let's see how tasks are instantiated. A task specifies its provider factory (with options) and its 
graph logic:
```hocon
factory = factory-name
config {
  # factory options
}
graph {
  # graph logic
}
```

In most cases the default task provider (`configuration`) is used, so the definition can be 
simplified to:
```hocon
tasks {
  # unique task name
  myTask {
    # graph logic
  }
}
```
> Note:
> Custom tasks providers can be easily added with custom [factories](https://github.com/Knotx/knotx-fragments/blob/master/handler/core/src/main/java/io/knotx/fragments/task/TaskProviderFactory.java) 
> that register in [Task Factory](https://github.com/Knotx/knotx-fragments/blob/master/handler/core/src/main/java/io/knotx/fragments/task/TaskFactory.java).

#### Graph
As already mentioned, the task logic is defined in the form of a directed graph. Moreover, this graph 
is acyclic and each of its nodes can be reached only from exactly one path (transition). These 
properties enable us to treat the Task as a tree structure. So we need to define the root node of 
the tree first.
```hocon
graph {
  # rootNodeDefinition
}
```
Each graph node sets fragment logic to perform and outgoing edges (called Transitions). So the 
`rootNodeDefinition` configuration looks like:
```hocon
graph {
  node { # required
     # node options (fragment processing logic)
  }
  onTransitions {
    # node outgoing edges
  }
}
```
There are two sections:
- `node` defines a fragment processing logic
- `onTransitions` is a map that represents outgoing edges in a graph

##### Node
Node's definition is are described [here](https://github.com/Knotx/knotx-fragments/tree/master/handler/engine#node).

Fragments Handler introduces defines custom node types that are finally converted to the 
[engine node types](https://github.com/Knotx/knotx-fragments/tree/master/handler/engine#node-types).
It allows to quickly introduce new node types, with different configuration options, without modifying
the engine.

Each node defines its custom factory. The configuration is simple::
```hocon
node {
  factory = factory-name
  config {
    # factory config
  }
}
```
The `factory` parameter specifies a node factory name, `config` contains all options passed to 
the factory. 

Fragments Handler provides two node implementations:
- **Action node** that represents simple steps in a graph such as integration with a data source
- **Subtasks node** that is a list of unnamed tasks (subtasks) that are evaluated in parallel

###### Action node

An *action node* declares an [action](#actions) to execute by its name:
```hocon
node {
  factory = action
  config {
    action = reference-to-action
    # onTransitions { }
  }
}
```
The above example specifies the action node that delegates processing to the `reference-to-action` 
action and has no transitions.

Knot.x allows simplifying action nodes declaration:
```hocon
action = reference-to-action
# onTransitions { }
```
A nice syntax sugar!

####### Logs
Action node appends a single [fragment's log](https://github.com/Knotx/knotx-fragments/tree/master/handler/engine#fragments-log) 
entry:

| Task       | Node identifier       | Node status | Transition | Node Log        |
|------------|-----------------------|-------------|------------|-----------------|
| `taskName` | `reference-to-action` | SUCCESS     | `_success` |  { }            |

with the custom [node log](https://github.com/Knotx/knotx-fragments/tree/master/handler/engine#node-log) syntax.

Let's assume that `NODE_LOG` is an action's node log with syntax:
```json5
{
  _alias: "reference-to-action",
  _logs: { 
    // action log here
  },
  _doAction: [
    // NODE_LOG, NODE_LOG, ...
  ]
}
```
So it supports both [actions](#actions) and [behaviours](#behaviours).

###### Subtasks node
Subtasks node is a node containing a list of subtasks. It evaluates all of them sequentially. 
However, all the operations are non-blocking, so it doesn't wait for previous subtasks to finish. 
Because of that, they are effectively executed in parallel

Moreover, a list of subtasks must fit the `F -> (F',T)` function. Each subtask has its fragment 
context, execute it's logic and update the fragment's payload (its own copy). Finally, when all
subtasks are completed, all payloads are merged and the new Fragment is returned.

> Note that [body](https://github.com/Knotx/knotx-fragments/blob/master/api/src/main/java/io/knotx/fragments/api/Fragment.java) modifications are not allowed because of the parallel execution of subtask nodes and the final `body` form cannot be determined. However, updating the fragment's `payload` is fine since all subtask nodes have their unique namespaces.

A subtask node definition is:
```hocon
node {
  factory = subtasks
  config {
    subtasks = [
      { 
        # subtask rootNodeDefinition
      },
      ...
    ]
  }
}
```
It follows the same simplification rules as action nodes:
```hocon
subtasks = [
  { 
    # subtask rootNodeDefinition
  },
  ...
]
```
Please note that `subtasks` is a list of nodes.

Let's see the example:
```hocon
subtasks = [
  { 
    action = book-rest-api
    # onTransitions { }
  },
  { 
    action = author-rest-api
    # onTransitions {}
  }
]
```
In the example above, the `book-rest-api` and `author-rest-api` actions are executed in parallel as 
two independent tasks (graphs) with one node (action).

See the [example section](#the-example) for a more complex scenario. Before we see the full 
power of graphs, we need to understand how nodes are connected.

####### Logs
Subtasks node appends a single [fragment's log](https://github.com/Knotx/knotx-fragments/tree/feature/%2347-action-log-structure/handler/engine#fragments-log) 
entry when all subgraphs are processed:

| Task       | Node identifier | Node status | Transition | Node Log        |
|------------|-----------------|-------------|------------|-----------------|
| `taskName` | `composite`     | SUCCESS     | `_success` |                 |

##### Transitions
A directed graph consists of nodes and edges. Edges are called transitions. Their configuration 
looks like:
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
> If a node responds with *_success* transition, but the transition is not configured, then 
>processing is finished.

> If a node responds with *_error* transition, but the transition is not configured, then an 
>exception is returned.

Nodes can declare custom transitions. Custom transitions allow to react to non standard situations 
such as data sources timeouts, fallbacks etc.

### The example
The example below collects data about the book and its authors from external APIs. Book and authors 
APIs accept ISBN and respond with JSON. We can invoke those APIs in parallel.
However, the book API does not contain the score. There is a separate service that accepts secret 
token from the book API and exposes the score data with XML syntax. When no secret token is found, 
then it responds with an empty score. 

The above logic can be easily transformed into the task:
```hocon
tasks {
  book-and-author-task {
    subtasks = [
      { # 1st subtask
        action = book-rest-api # HTTP Action
        onTransitions {
          _success {
            action = score-api # custom action
            onTransitions {
              noScore { # custom transition
                action = score-estimation # custom action
                # _success {} - subtask finished
              }
              # _success {} - subtask finished
            }   
          }
          _error {
            action = book-from-cache # custom action
          } 
        }
      },
      { # 2nd subtask
        action = author-rest-api # HTTP Action
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
The task root node is the subtasks node that declares two subtasks:
```hocon
action = book-rest-api
onTransitions {
  _success {
    action = score-api
    onTransitions {
      noScore {
        action = score-estimation
      }
    }   
  }
  _error {
    action = book-from-cache
  } 
}
```
and
```hocon
action = author-rest-api
```
Those subtasks are executed in parallel because there is no dependency between them. If any of them 
fails then the `book-and-author-fallback` fallback action is applied.
We used a similar strategy for the book API invocation. In this declarative way, we can easily handle 
timeouts and errors from APIs.
Please note that no error strategy has been defined for authors API yet. However, it can be easily 
configured in the future when business agrees on the fallback logic.


## Actions
Action defines action node logic. Actions can integrate with external data sources, do some fragments 
modifications or fetch data. A data source response is saved in a Fragment's payload (JSON object) 
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

Behaviours wrap other behaviours or simple actions and delegate a fragment to them (for processing). 
They can introduce some stability patterns such as retires, it means that they can call a wrapped 
Action many times.

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
