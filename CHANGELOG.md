# Changelog
All notable changes to `knotx-fragments` will be documented in this file.

## Unreleased
List of changes that are finished but not yet released in any final version.
- [PR-154](https://github.com/Knotx/knotx-fragments/pull/154) - Cleanup Fragments modules: renamed modules to be more self-descriptive. Remove hidden API dependencies.
- [PR-149](https://github.com/Knotx/knotx-fragments/pull/149) - Enable invalid fragments processing when a request param or header specified.
- [PR-148](https://github.com/Knotx/knotx-fragments/pull/148) - Add `Fragment JSON Consumer` supporting debug data for JSON responses.
- [PR-138](https://github.com/Knotx/knotx-fragments/pull/138) - Extract `Consumer API` and `Fragment HTML Body Writer`.
- [PR-136](https://github.com/Knotx/knotx-fragments/pull/136) - Extract Actions Core & API modules.
- [PR-120](https://github.com/Knotx/knotx-fragments/pull/120) - HTTP methods for HttpAction - support for POST/PUT/PATCH/DELETE/HEAD and sending body.
- [PR-100](https://github.com/Knotx/knotx-fragments/pull/100) - KnotxServer response configuration - wildcards, case-insensitive filtering allowed headers
- [PR-96](https://github.com/Knotx/knotx-fragments/pull/96) - HttpAction in knotx-fragments. Actions moved to a new module `knotx-fragments-handler-actions`.
- [PR-80](https://github.com/Knotx/knotx-fragments/pull/80) - Circuit breaker understands which custom transition mean error.
- [PR-84](https://github.com/Knotx/knotx-fragments/pull/84) - Adding node log to inline payload action.
- [PR-83](https://github.com/Knotx/knotx-fragments/pull/83) - Adding node log to inline body action.
- [PR-82](https://github.com/Knotx/knotx-fragments/pull/82) - Adding logging to in-memory cache action.
- [PR-60](https://github.com/Knotx/knotx-fragments/pull/60) - Adding node log to circuit breaker action. Enforce fallback on error.
- [PR-45](https://github.com/Knotx/knotx-fragments/pull/46) - Fragment Event Consumer mechanism implementation

## Version 2.1.0
- [PR-62](https://github.com/Knotx/knotx-fragments/pull/62) - It introduces task & graph node factories. It is connected with issue #49.
- [PR-51](https://github.com/Knotx/knotx-fragments/pull/51) - Introduces extendable task definition allowing to define different node types and custom task providers. Marks the `actions` task configuration entry as deprecated, introduces `subtasks` instread.
- [PR-56](https://github.com/Knotx/knotx-fragments/pull/56) - Makes composite node identifiers changeable. Renames `ActionNode` to `SingleNode`. 
- [PR-55](https://github.com/Knotx/knotx-fragments/pull/55) - Action log mechanism implementation. Renames `ActionFatalException` to `NodeFatalException`.

## 2.0.0
Initial open source release.
