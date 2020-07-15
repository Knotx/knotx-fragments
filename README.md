[![Build Status](https://dev.azure.com/knotx/Knotx/_apis/build/status/Knotx.knotx-fragments?branchName=master)](https://dev.azure.com/knotx/Knotx/_build/latest?definitionId=10&branchName=master)
[![CodeFactor](https://www.codefactor.io/repository/github/knotx/knotx-fragments/badge)](https://www.codefactor.io/repository/github/knotx/knotx-fragments)
[![codecov](https://codecov.io/gh/Knotx/knotx-fragments/branch/master/graph/badge.svg)](https://codecov.io/gh/Knotx/knotx-fragments)
[![Gradle Status](https://gradleupdate.appspot.com/Knotx/knotx-fragments/status.svg)](https://gradleupdate.appspot.com/Knotx/knotx-fragments/status)

# Knot.x Fragments

> While [Knot.x HTTP Server](https://github.com/Knotx/knotx-server-http) is a _"hearth"_ of Knot.x, 
> Fragments processing is its _"brain"_.

Knot.x Fragments is a Swiss Army knife for **integrating with various data sources**. It provides 
instruments that transform business use cases into implementation logic ready for evolution.

Knot.x Fragments is designed to build fault-tolerant, reactive, back-end integrations such as:
- [API Gateway](https://github.com/Knotx/knotx-example-project#api-gateway--web-api)
- Backend For Frontend (BFF) for single-page applications (SPA)
- [Web API](https://github.com/Knotx/knotx-example-project#api-gateway--web-api) (both REST and GraphQL)

Additionally, it still supports its original purpose which is templating solution 
that combines dynamic data (from external data sources, 3rd party API, etc.) with static content (HTML, 
JSON, PDF, etc.) that comes from various content stores (such as Wordpress, Drupal, Magnolia or 
Adobe Experience Manager). See the [example](https://github.com/Knotx/knotx-example-project#template-processing).

## Modules

- [Fragments API](https://github.com/Knotx/knotx-fragments/tree/master/api) - defines a [**Fragment**](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment) 
and a [Fragment Operation](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment-operation)
- [Actions](https://github.com/Knotx/knotx-fragments/tree/master/action) - extendable 
library of fragment operations that simplifies integration with APIs and provides stability 
patterns (e.g. [circuit breaker mechanism](https://github.com/Knotx/knotx-fragments/tree/master/action/library#circuit-breaker-behaviour))
- [Fragments Supplier](https://github.com/Knotx/knotx-fragments/tree/master/supplier) - converts an Http request into one or more [**Fragments**](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api)
- [Fragments Assembler](https://github.com/Knotx/knotx-fragments/tree/master/assembler) - merges Fragments into a single response
- [Task](https://github.com/Knotx/knotx-fragments/tree/master/task) - processes fragments and applies a configured business logic assigned to them

## How does it work

Knot.x Fragments is a set of [Handlers](https://github.com/Knotx/knotx-server-http/tree/master/api#routing-handlers)
that are plugged into the [Knot.x Server request processing](https://github.com/Knotx/knotx-server-http#how-does-it-work).

Fragments processing starts with [converting an HTTP request](#supply-fragments) to one or more
[Fragments](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api) that are 
then [evaluated](#evaluate-fragments) and eventually [combined into an HTTP response](#assemble-fragments).

### Supply Fragments

[**Fragments**](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api) 
are the result of a request being split (e.g. HTML markup) into smaller, independent parts by the
[Fragments Supplier](https://github.com/Knotx/knotx-fragments/tree/master/supplier).

![Fragments](https://github.com/Knotx/knotx-fragments/raw/master/assets/images/fragments_supplier.png)

### Evaluate Fragments

Each **Fragment** can specify a processing **Task** that points to a named, directed graph of **executable nodes**.

Each **node** transforms the Fragment's content, updates its payload and finally responds with **Transition**.

Nodes are connected with each other with Transitions, directed graph edges.

<img src="https://github.com/Knotx/knotx-fragments/raw/master/assets/images/graph_processing.png" width="700">

You may read more about it in the [Fragments Handler API](https://github.com/Knotx/knotx-fragments/tree/master/handler/api).

**Action** is a node with possible restrictions imposed. E.g. its execution
can be limited to a certain time. If this does not end within that time, Action will time out. 
In this case, the Action responds with an **error** Transition, which indicates that some **fallback node** can be applied.

<img src="https://github.com/Knotx/knotx-fragments/raw/master/assets/images/graph_processing_failure.png" width="500">

### Assemble Fragments

Finally, after all the Fragments were processed, they are combined into a single response by the 
[Fragments Assembler](https://github.com/Knotx/knotx-fragments/tree/master/assembler) handler.

## License
**Knot.x Fragments** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)

Icons come from [https://www.slidescarnival.com](https://www.slidescarnival.com/copyright-and-legal-information#license) and 
use [Creative Commons License Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).
