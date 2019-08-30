# Changelog
All notable changes to `knotx-fragments` will be documented in this file.

## Unreleased
List of changes that are finished but not yet released in any final version.
- [PR-60](https://github.com/Knotx/knotx-fragments/pull/60) - Adding node log to circuit breaker action. Enforce fallback on error.
- [PR-45](https://github.com/Knotx/knotx-fragments/pull/46) - Fragment Event Consumer mechanism implementation

## Version 2.1.0
- [PR-62](https://github.com/Knotx/knotx-fragments/pull/62) - It introduces task & graph node factories. It is connected with issue #49.
- [PR-51](https://github.com/Knotx/knotx-fragments/pull/51) - Introduces extendable task definition allowing to define different node types and custom task providers. Marks the `actions` task configuration entry as deprecated, introduces `subtasks` instread.
- [PR-56](https://github.com/Knotx/knotx-fragments/pull/56) - Makes composite node identifiers changeable. Renames `ActionNode` to `SingleNode`. 
- [PR-55](https://github.com/Knotx/knotx-fragments/pull/55) - Action log mechanism implementation. Renames `ActionFatalException` to `NodeFatalException`.

## 2.0.0
Initial open source release.