[![Build Status](https://dev.azure.com/knotx/Knotx/_apis/build/status/Knotx.knotx-fragments?branchName=master)](https://dev.azure.com/knotx/Knotx/_build/latest?definitionId=10&branchName=master)
[![CodeFactor](https://www.codefactor.io/repository/github/knotx/knotx-fragments/badge)](https://www.codefactor.io/repository/github/knotx/knotx-fragments)
[![codecov](https://codecov.io/gh/Knotx/knotx-fragments/branch/master/graph/badge.svg)](https://codecov.io/gh/Knotx/knotx-fragments)
[![Gradle Status](https://gradleupdate.appspot.com/Knotx/knotx-fragments/status.svg)](https://gradleupdate.appspot.com/Knotx/knotx-fragments/status)

# Knot.x Fragments

> While [Knot.x HTTP Server](https://github.com/Knotx/knotx-server-http) is a _"hearth"_ of Knot.x, Fragments processing 
is its _"brain"_.

Knot.x Fragments is a Swiss Army knife for **integrating with dynamic data sources**. It 
comes with **distributed systems stability patterns** such as a **circuit breaker** to handle different 
kinds of network failures. Thanks to those build-in mechanisms you can focus more on delivering
business logic and be ready to handle any unexpected integration problems.

Knot.x Fragments encourages to decompose business logic into a chain of simple steps that later 
can be wrapped with integration stability patterns without code changes. 
Besides, when the chain becomes more complex and additional failure scenarios are known, 
failure logic can be adjusted with fallback configuration (no changes in the business logic required).

Knot.x Fragments is designed to build fault-tolerant, back-end integrations such as:
- API Gateways, Backend For Frontend (BFF) for single-page applications (SPA), Web APIs
- documents processing (HTML, JSON, PDF etc) with a templating engine support

## Modules

- [Fragments Supplier](https://github.com/Knotx/knotx-fragments/tree/master/supplier) - converts an Http request into one or more [**Fragments**](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api)
    - [HTML Splitter](https://github.com/Knotx/knotx-fragments/tree/master/supplier/html-splitter)
    - [Single Fragment Supplier](https://github.com/Knotx/knotx-fragments/tree/master/supplier/single-fragment)
- [Fragments Handler](https://github.com/Knotx/knotx-fragments/tree/master/handler) - configures tasks for fragments and delegates processing to Engine 
    - [Fragment Execution Log Consumer](https://github.com/Knotx/knotx-fragments/tree/master/handler/consumer) - exposes data from evaluated fragments
- [Fragments Engine](https://github.com/Knotx/knotx-fragments/tree/master/engine) - evaluates fragments (tasks) with map-reduce nature
- [Fragments Assembler](https://github.com/Knotx/knotx-fragments/tree/master/assembler) - merges Fragments into a single response

Each module contains its own documentation inside.

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

### Example: HTML template processing

Read more about configuring HTML template processing in the [Knot.x Example Project](https://github.com/Knotx/knotx-example-project/tree/master/template-processing).

## License
**Knot.x Fragments** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)

Icons come from [https://www.slidescarnival.com](https://www.slidescarnival.com/copyright-and-legal-information#license) and 
use [Creative Commons License Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).
