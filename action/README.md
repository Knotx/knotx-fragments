# Fragments Actions
It is a tool-kit for integrating with different data sources (e.g. HTTP-based APIs). It 
implements stability patterns to prevent/handle network issues.

## Modules:
- [API](https://github.com/Knotx/knotx-fragments/tree/master/action/api) - provides classes and interfaces required during custom actions implementation
- [Core](https://github.com/Knotx/knotx-fragments/tree/master/action/core) - initializes actions using the [action provider](https://github.com/Knotx/knotx-fragments/tree/master/action/core#action-provider)
- [Library](https://github.com/Knotx/knotx-fragments/tree/master/action/library) - the core actions library

## How to start?
- add required dependencies to the classpath:
  - `knotx-fragments-action-core.X.Y.Z.jar`
  - `knotx-fragments-action-library.X.Y.Z.jar`
- define an [action](#action) identified by name, e.g.
  ```hocon
  fetch-user {
    factory = http
    config.endpointOptions {
      path = "/service/user"
      domain = localhost
      port = 1024
      allowedRequestHeaders = ["Content-Type"]
    }
  }  
- use an [action provider](https://github.com/Knotx/knotx-fragments/tree/master/action/core#action-provider) 
  to execute action logic (that updates a [fragment](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment)
  passed in `fragmentContext`)
  ```java
  provider.get("fetch-user").apply(fragmentContext, resultHandler)
  ```

## How does it work?
[Action](#action) is a ready to use [fragment operation](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment-operation) 
created by an [action factory](https://github.com/Knotx/knotx-fragments/tree/master/action/api#action-factory). Action may be parametrized with JSON (`config`). 
You can use action factories to create actions, however, we recommend using the [action provider](https://github.com/Knotx/knotx-fragments/tree/master/action/core#action-provider) 
that would hide the complexity of initialization (maintaining stateful actions, combining actions 
with their behaviours).

### Action
[Action](https://github.com/Knotx/knotx-fragments/tree/master/action/api#action) can invoke an API 
and save the response body in a fragment's payload or simply modify its body.

Action may be decorated with [behaviours](#behaviour). Actions deployed on the [Vert.x Event Bus](https://vertx.io/docs/vertx-core/java/#event_bus) 
are called [Knots](https://github.com/Knotx/knotx-fragments/tree/master/action/api#knot).

Below there is a list of core actions:
- [HTTP Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#http-action) - calls an external Web API and stores a response in a fragment's payload
- [Inline Body Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#inline-body-action) - replaces a fragment's body with the configured value
- [Inline Payload Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#inline-payload-action) - adds the configured JSON data into a fragment's payload
- [Payload To Body Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#payload-to-body-action) - rewrites a fragment's payload to the body

> Please note that action can be either **stateless** or **stateful**. It fully depends on its implementation.

### Behaviour
Behaviour is an [action](#action) wrapper, that enriches the original action with some 
cross-cutting functionality e.g HTTP operation can gain the circuit breaker pattern mechanism behaviour.

Below there is a list of core behaviours:
- [Circuit Breaker Behaviour](https://github.com/Knotx/knotx-fragments/tree/master/action/core#circuit-breaker-behaviour) - it is a kind of quarantine for actions, it uses the [Vert.x Circuit Breaker](https://vertx.io/docs/vertx-circuit-breaker/java/) implementation
- [In-memory Cache Behaviour](https://github.com/Knotx/knotx-fragments/tree/master/action/core#in-memory-cache-behaviour) - caches a fragment's payload to reduce the number of action invocations
