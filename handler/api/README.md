# Fragments Handler API

## Action
**Action** is a simple function that operates on a [Fragment](https://github.com/Knotx/knotx-fragments-handler/tree/master/api#knotx-fragment-api) 
(which is part of a [`FragmentContext`](https://github.com/Knotx/knotx-fragments-handler/blob/master/api/docs/asciidoc/dataobjects.adoc#FragmentContext)).
The result of applying Action logic is the [`FragmentResult`](https://github.com/Knotx/knotx-fragments-handler/blob/master/api/docs/asciidoc/dataobjects.adoc#FragmentResult)
which contains the new Fragment and a Transition (which determines an edge in the processing graph).

In other words, you should understand applying an Action as performing the function:
`<F> -> <F' + T>`, where:
 - `F` is a *Fragment*,
 - `F'` is modified *Fragment*,
 - `T` is a *Transition*.

## Knot
**Knot** is a scalable **Action** that is available on the [Vert.x Event Bus](https://vertx.io/docs/vertx-core/java/#event_bus).

Read more about the API Data Objects in the [Data Object docs](https://github.com/Knotx/knotx-fragments-handler/blob/master/api/docs/asciidoc/dataobjects.adoc).