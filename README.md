[![][travis img]][travis]
[![][license img]][license]

# Knot.x Fragments

> While [Knot.x HTTP Server](https://github.com/Knotx/knotx-server-http) is a "hearth" of Knot.x, Fragments processing 
is its "brain".

Knot.x Fragments is an "army swiss knife" when it comes to **integrating with dynamic data sources**. It 
comes with **distributed systems stability patterns** such as a **circuit breaker mechanism** to handle different 
kinds of network failures. Thanks to those build-in mechanisms you can focus more on delivering
business logic and be ready to handle any unexpected integration problems.

Knot.x Fragments encourages you to decompose your business logic into a chain of simple steps
that later on can be wrapped with integration stability patterns without code changes. In addition, 
when the chain becomes more and more complex and more failure scenarios are known, you can adjust 
failure logic with fallback configuration (no changes in the business logic required).

Knot.x Fragments is designed to build fault-tolerant, back-end integrations such as:
- Gateway APIs
- Web APIs
- template processing (HTML markup, JSON, PDF etc). 


## How does it work

Knot.x Fragments is a set of [Handlers](https://github.com/Knotx/knotx-server-http/tree/master/api#routing-handlers)
that are plugged into the [Knot.x Server request processing](https://github.com/Knotx/knotx-server-http#how-does-it-work).

When HTTP request comes to Knot.x, Fragments processing starts with [adapting the request](#supply-fragments) to one or more
Fragments that are [evaluated](#evaluate-fragments) later and finally [combined into the response](#assemble-fragments).

### Supply Fragments

[**Fragments**](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api) 
are the result of breaking the request (in this example it is the HTML markup) into smaller, independent parts by the
[Fragments Supplier](https://github.com/Knotx/knotx-fragments/tree/master/supplier).

![Fragments](https://github.com/Knotx/knotx-fragments/raw/master/assets/images/fragments_supplier.png)

### Evaluate Fragments

Each **Fragment** may specify a processing **Task** that points to a named, directed graph of **Actions**.

Each **Action** transforms the Fragment's content and/or updates its payload. 

Fragment's path in the Task graph is defined by Action's output, called **Transition**.

<img src="https://github.com/Knotx/knotx-fragments/raw/master/assets/images/graph_processing.png" width="700">

You may read more about it in the [Fragments Handler API](https://github.com/Knotx/knotx-fragments/tree/master/handler/api).

**Action** is a simple function (business logic) with possible restrictions imposed. E.g. function execution
can be limited to a certain amount of time. If it will not end in that time, Action times out. 
In that case, Action's output is **error Transition** and some **fallback Action** may be applied.

<img src="https://github.com/Knotx/knotx-fragments/raw/master/assets/images/graph_processing_failure.png" width="500">

Additionally, **Actions** may be executed in **parallel** when they are independent. More details can 
be found [here](https://github.com/Knotx/knotx-fragments/tree/master/handler/engine).

You may read more about actions restrictions and implementations of Actions delivered with this 
module in the [Fragments Handler](https://github.com/Knotx/knotx-fragments/tree/master/handler)

### Assemble Fragments

Finally, after all the Fragments were processed, they are combined into a single response by the 
[Fragments Assembler](https://github.com/Knotx/knotx-fragments/tree/master/assembler) handler.

### Example: HTML template processing

Read more about configuring HTML template processing in the [Knot.x Example Project](https://github.com/Knotx/knotx-example-project/tree/master/template-processing).


## Modules 

It contains such modules as:
- [Fragments Supplier](https://github.com/Knotx/knotx-fragments/tree/master/supplier) - converts a HTTP request into one or more [**Fragments**](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api)
    - [HTML Splitter](https://github.com/Knotx/knotx-fragments/tree/master/supplier/html-splitter)
    - [Single Fragment Supplier](https://github.com/Knotx/knotx-fragments/tree/master/supplier/single-fragment)
- [Fragments Handler](https://github.com/Knotx/knotx-fragments/tree/master/handler) - evaluates Tasks assigned to Fragments
- [Fragments Assembler](https://github.com/Knotx/knotx-fragments/tree/master/assembler) - merges Fragments into one a single response

Each module contains its own documentation inside.


## License
**Knot.x Fragments** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)

Icons come from [https://www.slidescarnival.com](https://www.slidescarnival.com/copyright-and-legal-information#license) and 
use [Creative Commons License Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).

[travis]:https://travis-ci.com/Knotx/knotx-fragments
[travis img]:https://travis-ci.com/Knotx/knotx-fragments.svg?branch=master

[license]:https://github.com/Knotx/knotx-fragments/blob/master/LICENSE
[license img]:https://img.shields.io/badge/License-Apache%202.0-blue.svg
