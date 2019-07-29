[![][travis img]][travis]
[![][license img]][license]

# Knot.x Fragments
This repository contains all modules that are the core of the [Knot.x Fragments processing](https://github.com/Knotx/knotx-fragments#how-does-it-work):
- [Fragments Supplier](https://github.com/Knotx/knotx-fragments/tree/master/supplier)
    - [HTML Splitter](https://github.com/Knotx/knotx-fragments/tree/master/supplier/html-splitter)
    - [Single Fragment Supplier](https://github.com/Knotx/knotx-fragments/tree/master/supplier/single-fragment)
- [Fragments Handler](https://github.com/Knotx/knotx-fragments/tree/master/handler)
- [Fragments Assembler](https://github.com/Knotx/knotx-fragments/tree/master/assembler)

## How does it work

The example below depicts a page containing independent components which render data coming from 
external sources.

![Page with components](https://github.com/Knotx/knotx-fragments/raw/master/assets/images/case.png)

### Supply Fragments

[**Fragments**](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api) 
are the result of breaking the request (in this example it is the HTML markup) into smaller, independent parts by the
[Fragments Supplier](https://github.com/Knotx/knotx-fragments/tree/master/supplier).

![Fragments](https://github.com/Knotx/knotx-fragments/raw/master/assets/images/fragments_supplier.png)

Each **Fragment** may specify a processing **Task** that points to a named, directed graph of **Actions**.

Each **Action** transforms the Fragment's content and/or updates its payload. 

Fragment's path in the Task graph is defined by Action's output, called **Transition**.

<img src="https://github.com/Knotx/knotx-fragments/raw/master/assets/images/graph_processing.png" width="700">

You may read more about it in the [Fragments Handler API](https://github.com/Knotx/knotx-fragments/tree/master/handler/api).

### Evaluate Fragments

**Action** is a simple function (business logic) with possible restrictions imposed. E.g. function execution
can be limited to a certain amount of time. If it will not end in that time, Action times out. 
In that case, Action's output is **error Transition** and some **fallback Action** may be applied.

<img src="https://github.com/Knotx/knotx-fragments/raw/master/assets/images/graph_processing_failure.png" width="500">

Additionally, **Actions** may be composed into a **Composed Structure** that will be executed in **parallel** when they
are independent.

You may read more about actions restrictions and implementations of Actions delivered with this 
module in the [Fragments Handler](https://github.com/Knotx/knotx-fragments/tree/master/handler)

### Join Fragments

Finally, after all the Fragments were processed, they are combined into a single response by the 
[Fragments Assembler](https://github.com/Knotx/knotx-fragments/tree/master/assembler) handler.

### Fragments Engine
The diagram below depicts Fragments Engine logic (map-reduce).

![Node with exits](https://github.com/Knotx/knotx-fragments/raw/master/assets/images/all_in_one_processing.png)

Read more about it [here](https://github.com/Knotx/knotx-fragments/tree/master/handler/engine).

## License
**Knot.x Fragments** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)

Icons come from [https://www.slidescarnival.com](https://www.slidescarnival.com/copyright-and-legal-information#license) and 
use [Creative Commons License Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).

[travis]:https://travis-ci.com/Knotx/knotx-fragments
[travis img]:https://travis-ci.com/Knotx/knotx-fragments.svg?branch=master

[license]:https://github.com/Knotx/knotx-fragments/blob/master/LICENSE
[license img]:https://img.shields.io/badge/License-Apache%202.0-blue.svg
