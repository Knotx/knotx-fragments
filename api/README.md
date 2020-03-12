# Fragments API
This repository contains base models related to [Fragments](#fragment) and [Operations](#fragment-operation).

## Fragment
A **Fragment** is a piece of any kind of document that can be processed independently (e.g. 
an HTML snippet that represents the shopping cart or a JSON containing person's bookshelf).

Originally it may come from breaking a bigger document into smaller pieces.
A fragment is not associated in any way with any particular format, so examples of fragments may be:
- piece of HTML markup,
- piece of PDF file,
- piece of Office document (e.g. Word paragraph or Excel row),
- some (binary) part of an image.

It contains the id, type, configuration, body and payload. Read more [here](https://github.com/Knotx/knotx-fragments/blob/master/api/docs/asciidoc/dataobjects.adoc#fragment).

## Fragment Operation
**Fragment Operation** is a simple function that transforms a [Fragment](#fragment) into the new Fragment and provides the status of that transition (e.g. success/error).

An argument for the Fragment Operation is a [`FragmentContext`](https://github.com/Knotx/knotx-fragments/blob/master/api/docs/asciidoc/dataobjects.adoc#fragmentcontext) (it contains the Fragment itself and some additional context).
The result of applying operation logic is the [`FragmentResult`](https://github.com/Knotx/knotx-fragments/blob/master/api/docs/asciidoc/dataobjects.adoc#fragmentresult)
which contains the new fragment and a transition (which determines an operation state).

In other words, if you think of this as a mathematical function, its signature can be simply explained as **`F -> (F', T, L)`**, 
where `F` is a fragment, `F'` is a modified fragment, `T` is a transition and `L` is an operation log.

It has also its reactive version - `io.knotx.reactivex.fragments.api.FragmentOperation`.
