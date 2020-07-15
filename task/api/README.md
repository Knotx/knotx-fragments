# Task API
This repository contains a [task](#task) definition details. 

If you are looking for information about:
- task declaration syntax, check the [Task Factories](https://github.com/Knotx/knotx-fragments/tree/master/task/factory)
- task processing details (such as error handling etc.), check the [Task Engine](https://github.com/Knotx/knotx-fragments/tree/master/task/engine)

## Task
Task transforms a [fragment](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment) into 
a new one. It declares [fragment operations](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment-operation) 
to be performed and connections between them. It reflects that a task is a graph, whose [nodes](#node) 
are fragment operations linked by edges (called [transitions](#transition)). So a task is a directed 
graph of nodes. Besides, each node can be reached only from exactly one path (transition), meaning the graph 
is acyclic. It's basically a tree.

The graph below depicts nodes, represented by circles (`A, B, C`), and transitions illustrated as 
arrows (`B transition`, `C transition`).

![Task](assets/images/graph.png)

Tasks allow defining more [complex structures](#composite-node) such as a list of subtasks (subgraphs).

### Node
Node has a single input and declares zero-to-many outputs (transitions). There are two base node 
types: a [single node](#single-node) and a [composite node](#composite-node).

See also the [Actions Library](https://github.com/Knotx/knotx-fragments/tree/master/action/library), 
that provides the most common node's implementations, e.g `HTTP Action`.

#### Single Node
A single node is a [fragment operation](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment-operation) 
that performs some fragment transformation logic. It can change both fragment's body and payload.

The diagram below represents the single node (A) with one input and three outputs (`B transition`, 
`C transition`, `D transition`).

![Single node](assets/images/single-node.png)

The example of this node is calling an authentication RESTful API. The node implements a communication 
logic and reacts to different API responses such as HTTP 200/401/404 status codes.

![Single node example](assets/images/single-node-example.png)

Each status code may represent various decisions such as a successful authentication, a user not 
found or even expired password.

#### Composite Node
Composite node defines a list of subgraphs to evaluate. It enables `parallel` processing of independent 
subgraphs (e.g. calling two external independent data sources). Each subgraph gets a fragment copy and 
updates a fragment's payload. When all subgraphs end, then the reduction phase happens during which 
all subgraphs results are merged (all copies' payloads).

> Important note!
> Nodes inside the composite node may only append the Fragment's payload and should not modify 
> the Fragment's body. 
> It's because there is no guarantee which node finishes processing earlier and what will be the final body output

Composite node may consist of 
- list of subgraphs of single nodes:<br/>
  ![Composite node](assets/images/composite.png)<br/>
  The diagram above represents a composite node that consists of:
  - subgraph starting with `A` (`A` -> `B`)
  - subgraph starting with `C`

- other composite nodes (or a mix of both): <br/>
  ![Nested composite nodes](assets/images/nested-composite.png)<br/>
  The diagram above represents a composite node that consists of: 
  - subgraph starting with `A` node
  - subgraph that is also a composite node (that consists of two subgraphs starting with `C` and `D` nodes).

so it is very elastic.

Despite the fact, that composite node is more like subgraph structure its general purpose is to 
transform one fragment into a new one. Thinking more abstractly, it follows the same rules as 
[fragment operations](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment-operation).

However, a composite node can respond with only the [success](#success-transition) and 
[error](#error-transition) transitions. Imagine the situation when each subgraph responds with a 
[custom](#custom-transition) transition. When all composite processing ends, we try to reduce the 
subgraphs' results, but we can not determine which custom transition is more significant.

This observation involves the following rules:
> Composite node responds with [success](#success-transition) when all subgraphs (last nodes) respond with [success](#success-transition) transition.

> Composite node responds with [error](#error-transition) when any subgraph (last node) responds with [error](#success-transition) or [custom](#custom-transition) transition.  

#### Node log
Every node can prepare some data that describes what happened during its processing. It is a JSON 
structure, called a node log. The node log syntax depends on its implementation.

### Transition
Transition is a named graph edge. Node can respond with any transition, it is a text value.

#### Success transition
It indicates that operation completes successfully (no exception). Its value is `_success`.

#### Error transition
It means that operation has thrown an exception. Its value is `_error`.

#### Custom transition
It is any transition besides the [success transition](#success-transition) and [error transition](#error-transition). 
It means that each node can declare its own subset of outputs.

> Custom transitions can be used also to react to non-standard situations such as data sources timeouts, fallbacks etc. 

### Task stop conditions
There are three important rules to remember:
- If a node responds with the [success](#success-transition) transition, but the transition is not configured, then 
processing of the graph/subgraph is finished.
- If a node responds with [error](#error-transition) transition, but the transition is not configured, then an 
exception is returned.
- If a node responds with a not configured transition, the "Unsupported Transition" error occurs.
