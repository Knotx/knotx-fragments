# Default Task Factory
The default [task factory](https://github.com/Knotx/knotx-fragments/blob/master/task/factory/api#task-factory) loads a task definition from the configuration and creates a [task](https://github.com/Knotx/knotx-fragments/blob/master/task/api#task)
based on it. It uses the [actions' library](https://github.com/Knotx/knotx-fragments/blob/master/action#task)
to do operations on the fragment (such as fetching data from 3rd party APIs).

## How does it work
It registers extendable graph node factories (one of them uses Actions), delegates node initialization 
to them and joins all nodes with transitions.

Factory implements the [task factory interface](https://github.com/Knotx/knotx-fragments/blob/master/task/factory/api#task-factory). 
It is a java library (a JAR file) that can be loaded using [Java SPI](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html). 

## How to configure
For all configuration fields and their defaults consult [DefaultTaskFactoryConfig](https://github.com/Knotx/knotx-fragments/blob/master/task/factory/default/docs/asciidoc/dataobjects.adoc#defaulttaskfactoryconfig).

In general:
- it defines supported [tasks (by name)](#tasks) containing graph configurations in `tasks`
- it converts the graph configurations into tasks using [node factories](#node-factory) defined in `nodeFactories`

### Tasks
[Tasks](https://github.com/Knotx/knotx-fragments/tree/master/task/api#task) are configured in 
the form of a dictionary (`taskName -> graph logic`):
```hocon
tasks {
  myTask { # unique task name
    # graph configuration
  }
}
```

A graph configuration starts with a root [node](#node) definition. Each node sets some logic to perform 
over a fragment and outgoing edges (called [transitions](#transition)). So there are two sections:
```hocon
node {
  # node options (fragment processing logic)
}
onTransitions {
  # node outgoing edges
}
```

The `node` section contains some node options (e.g action name), the `onTransitions` defines transitions
and assigned next nodes.

#### Node
The `node` section provides details on how the [node](https://github.com/Knotx/knotx-fragments/tree/master/task/api#node) 
is instantiated.

The default nodes:
- [action node](#action-node)
- [subtasks node](#subtasks-node)

are initialized by [node factories](#node-factory).

##### Action node
An *action node* declares an action name to execute. An action name points to the action configured
in the [action node factory](#action-node-factory).

###### Full syntax
```hocon
node {
  factory = action
  config {
    action = action-name
  }
}
# onTransitions { }
```

###### Short syntax
```hocon
action = action-name
# onTransitions { }
```

Please note that all options despite of `node.config.action` are skipped.

##### Subtasks node
Subtasks node is a node containing a list of subtasks. It evaluates all of them sequentially. 
However, all the operations are non-blocking, so it doesn't wait for previous subtasks to finish. 
Because of that, they are effectively executed in parallel.

Each subtask has its fragment context, execute its logic and update the fragment's payload (its 
copy). Finally, when all subtasks are completed, all payloads are merged and the new Fragment is 
returned.

> Note that [body](https://github.com/Knotx/knotx-fragments/blob/master/api/src/main/java/io/knotx/fragments/api/Fragment.java) modifications are not allowed because of the parallel execution of subtask nodes and the final `body` form cannot be determined. However, updating the fragment's `payload` is fine since all subtask nodes have their unique namespaces.

>  Note that for some complex scenarios nested subtasks are supported.

###### Full syntax
```hocon
node {
  factory = subtasks
  config {
    subtasks = [
      # subtask - subgraph definition
      {
        # node {}
        # onTransitions { }
      },
      ...
    ]
  }
}
# onTransitions { }
```
Please note that `subtasks` is a list of sub-graphs (a list of sub-graph root nodes with transitions).

###### Short syntax
```hocon
subtasks = [
  # subtask - subgraph definition
  {
    # node {}
    # onTransitions { }
  },
  ...
]
```

#### Transition
A directed graph consists of nodes and edges. Edges are called 
[transitions](https://github.com/Knotx/knotx-fragments/tree/master/task/api#transition). 

Their configuration looks like:
```hocon
onTransitions {
  _success {
    # next node when _success transition
    # node {}
    # onTransitions { }
  }
  _error {
    # next node when error occurs
    # node {}
    # onTransitions { }
  }
  customTransition {
    # next node when custom transition
    # node {}
    # onTransitions { }
  }
}
```

#### Example
Let's see the example using the short syntax mixing subtasks and action nodes:
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
# onTransitions { }
```
In the example above, the `book-rest-api` and `author-rest-api` actions are executed in parallel as 
two independent sub-tasks (sub-graphs).

See the [complex example section](#complex-example) for a more advanced scenario.

### Node factory
The default task factory initializes [nodes](#node) with node factories. It guarantees a separation 
between a task and its "body" (node processing logic).

The `nodeFactories` is an array of [NodeFactoryOptions](https://github.com/Knotx/knotx-fragments/blob/master/task/factory/default/docs/asciidoc/dataobjects.adoc#nodefactoryoptions) 
that contains configs for registered node factories.

The default node factories:
- [action node factory](#action-node-factory) using the actions' library
- [subtasks node factory](#subtasks-node-factory) that gives parallel processing possibilities

All node factory implementations are registered using a simple service-provider loading facility - 
[Service Loader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).

#### Action node factory
It is implemented by the [ActionNodeFactory](https://github.com/Knotx/knotx-fragments/blob/master/task/factory/default/src/main/java/io/knotx/fragments/task/factory/generic/node/NodeFactoryOptions.java) 
class. Its name is `action`. It is configured with [ActionNodeFactoryConfig](https://github.com/Knotx/knotx-fragments/blob/master/task/factory/default/docs/asciidoc/dataobjects.adoc#actionnodefactoryconfig).

In general:
- it declares [actions](https://github.com/Knotx/knotx-fragments/tree/master/action/api#action) 
(by name) used in [action nodes](#action-node)
- node log level that is passed to actions

Its configuration looks like:
```hocon
nodeFactories = [
  {
    factory = action
    config {
      actions {
        action-name {
          # action configuration
          # factory {}
          # config {}
        }
      }
      logLevel = error
    }
  }
]
```

##### Logs
Action node produces the log with syntax:
```json5
{
  _alias: "reference-to-action",
  _logs: {
    // action log goes here
  },
  _doAction: [
    // NODE_LOG, NODE_LOG, ...
  ]
}
```
> Please note that Action log syntax is defined [here](https://github.com/Knotx/knotx-fragments/tree/master/action/api#log)

#### Subtasks node factory
A subtask is nothing else than a subgraph defined inside the task.
Creating subtasks is implemented in the [SubtasksNodeFactory](https://github.com/Knotx/knotx-fragments/blob/master/handler/core/src/main/java/io/knotx/fragments/task/factory/node/subtasks/SubtasksNodeFactory.java) 
class. Its name is `subtasks`. Its configuration is empty.

Its configuration looks like:
```hocon
nodeFactories = [
  {
    factory = subtasks
  }
]
```

##### Logs
Node log for subtasks nodes is empty.

## Complex example
The example below collects data about the book and its authors from external APIs. Book and authors 
APIs accept ISBN and respond with JSON. We can invoke those APIs in parallel.
However, the book API does not contain the score. There is a separate service that accepts secret 
token from the book API and exposes the score data with XML syntax. When no secret token is found, 
then it responds with an empty score. 

The above logic can be easily transformed into the task:
```hocon
tasks {
  book-and-author-task {
    config {
      actions {
        book-rest-api {}
        author-rest-api {}
        book-from-cache{}
        score-api {}
        score-estimation {}
      }
    }
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
