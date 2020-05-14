# Changelog
All notable changes to `knotx-fragments` will be documented in this file.

## Unreleased
List of changes that are finished but not yet released in any final version.
- [PR-154](https://github.com/Knotx/knotx-fragments/pull/154) - Cleanup Fragments modules: renamed modules (`Actions` and all Task related once) to be more self-descriptive. Remove hidden API dependencies.
- [PR-149](https://github.com/Knotx/knotx-fragments/pull/149) - Enable invalid fragments processing when a request param or header specified.
- [PR-148](https://github.com/Knotx/knotx-fragments/pull/148) - Add [Fragment JSON Execution Log Consumer](https://github.com/Knotx/knotx-fragments/tree/master/task/handler/log/json) supporting debug data for JSON responses.
- [PR-138](https://github.com/Knotx/knotx-fragments/pull/138) - Extract [Fragment Execution Log Consumer API](https://github.com/Knotx/knotx-fragments/tree/master/task/handler/log/api) and [Fragment HTML Body Writer](https://github.com/Knotx/knotx-fragments/tree/master/task/handler/log/html).
- [PR-136](https://github.com/Knotx/knotx-fragments/pull/136) - Extract [Actions API & Core](https://github.com/Knotx/knotx-fragments/tree/master/action) modules.
- [PR-119](https://github.com/Knotx/knotx-fragments/pull/119) - Introduce [Fragment Operation](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment-operation) to link [Action](https://github.com/Knotx/knotx-fragments/tree/master/action/api#action) and [Task Action Node](https://github.com/Knotx/knotx-fragments/tree/master/task/factory/default#action-node-factory).
- [PR-120](https://github.com/Knotx/knotx-fragments/pull/120) - HTTP methods for [Http Action](https://github.com/Knotx/knotx-fragments/tree/master/action/library#http-action) - support for `POST`/`PUT`/`PATCH`/`DELETE`/`HEAD` and sending body.
- [PR-106](https://github.com/Knotx/knotx-fragments/pull/106) - Extract [Task Engine](https://github.com/Knotx/knotx-fragments/tree/master/task/engine).
- [PR-100](https://github.com/Knotx/knotx-fragments/pull/100) - KnotxServer response configuration - wildcards, case-insensitive filtering allowed headers
- [PR-99](https://github.com/Knotx/knotx-fragments/pull/99) - [Http Action](https://github.com/Knotx/knotx-fragments/tree/master/action/library#http-action) instances can be reused between requests.
- [PR-96](https://github.com/Knotx/knotx-fragments/pull/96) - Move [Http Action](https://github.com/Knotx/knotx-fragments/tree/master/action/library#http-action) from [Knot.x Data Bridge](https://github.com/Knotx/knotx-data-bridge) to Fragments repository. Actions moved to a new module `knotx-fragments-action-library`.
- [PR-80](https://github.com/Knotx/knotx-fragments/pull/80) - [Circuit Breaker Behaviour](https://github.com/Knotx/knotx-fragments/tree/master/action/library#circuit-breaker-behaviour) understands which custom transitions mean error.
- [PR-84](https://github.com/Knotx/knotx-fragments/pull/84) - Add the [action log](https://github.com/Knotx/knotx-fragments/tree/master/action/api#action-log) support [Inline Payload Action](https://github.com/Knotx/knotx-fragments/tree/master/action/library#inline-payload-action).
- [PR-83](https://github.com/Knotx/knotx-fragments/pull/83) - Add the [action log](https://github.com/Knotx/knotx-fragments/tree/master/action/api#action-log) support to [Inline Body Action](https://github.com/Knotx/knotx-fragments/tree/master/action/library#inline-body-action).
- [PR-82](https://github.com/Knotx/knotx-fragments/pull/82) - Add the [action log](https://github.com/Knotx/knotx-fragments/tree/master/action/api#action-log) support to [In-memory Cache Behaviour](https://github.com/Knotx/knotx-fragments/tree/master/action/library#in-memory-cache-behaviour).
- [PR-60](https://github.com/Knotx/knotx-fragments/pull/60) - Add the [action log](https://github.com/Knotx/knotx-fragments/tree/master/action/api#action-log) support to [Circuit Breaker Behaviour](https://github.com/Knotx/knotx-fragments/tree/master/action/library#circuit-breaker-behaviour). Enforce the `fallback` on error strategy.
- [PR-45](https://github.com/Knotx/knotx-fragments/pull/46) - [Fragment Event Consumer](https://github.com/Knotx/knotx-fragments/tree/master/task/handler/log) mechanism implementation.

## Version 2.1.0
- [PR-62](https://github.com/Knotx/knotx-fragments/pull/62) - It introduces task & graph node factories. It is connected with issue #49.
- [PR-51](https://github.com/Knotx/knotx-fragments/pull/51) - Introduces extendable task definition allowing to define different node types and custom task providers. Marks the `actions` task configuration entry as deprecated, introduces `subtasks` instread.
- [PR-56](https://github.com/Knotx/knotx-fragments/pull/56) - Makes composite node identifiers changeable. Renames `ActionNode` to `SingleNode`. 
- [PR-55](https://github.com/Knotx/knotx-fragments/pull/55) - Action log mechanism implementation. Renames `ActionFatalException` to `NodeFatalException`.

## 2.0.0
Initial open source release.
