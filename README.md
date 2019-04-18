# Fragments Handler API
It is a [**Handler**](https://github.com/Knotx/knotx-server-http/tree/master/api#routing-handlers)
that processes a request during HTTP Server request processing.
It operates on [**Fragments**](https://github.com/Knotx/knotx-fragment-api#knotx-fragment-api)
 that are result of breaking request into smaller, independent parts. 

![Node with exits](assets/images/case.png)

## How does it works

Each **Fragment** may specify a processing **Task** that points to a named, directed graph of **Actions**.

Each **Action** transforms Fragment's content and/or updates its payload. 

Fragment's path in the Task graph is defined by Action's output, called **Transition**.

<img src="https://github.com/Knotx/knotx-fragments-handler/raw/master/assets/images/graph_processing.png" width="700">

---

**Action** is a simple function (business logic) with possible restrictions imposed. E.g. function execution
can be limited to a certain amount of time. If it will not end in that time, Action times out. 
In that case, Action's output is **error Transition** and some **fallback Action** may be applied.

<img src="https://github.com/Knotx/knotx-fragments-handler/raw/master/assets/images/graph_processing_failure.png" width="500">

Additionally, **Actions**  may be composed into **Composed Structure** that will be executed in parallel when they
all independent.

---

The diagram belows depicts Fragments Engine logic (map-reduce).

![Node with exits](assets/images/all_in_one_processing.png)

## Licence
**Knot.x Fragments Handler** is licensed under the [Apache License, Version 2.0 (the "License")](https://www.apache.org/licenses/LICENSE-2.0.txt)

Icons come from [https://www.slidescarnival.com](https://www.slidescarnival.com/copyright-and-legal-information#license) and 
use [Creative Commons License Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).
