# Fragments Actions
It is a tool-kit for integrating with different data sources (e.g. HTTP-based APIs). It 
implements stability patterns to prevent/handle network issues.

Modules:
- [API](https://github.com/Knotx/knotx-fragments/tree/master/action/api) - provides classes and interfaces required during custom actions implementation
- [Core](https://github.com/Knotx/knotx-fragments/tree/master/action/core) - initializes actions using the [action provider](#action-provider)
- [Library](https://github.com/Knotx/knotx-fragments/tree/master/action/library) - a library of actions' implementations

## How to start?
- add `knotx-fragments-action-core.X.Y.Z.jar` and `knotx-fragments-action-library.X.Y.Z.jar` to the classpath
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
- use an [action provider](#action-provider) to execute action logic (that updates a [fragment](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment))
  ```java
  provider.get("fetch-user").apply(fragmentContext, resultHandler)
  ```

## How does it work?
[Action](#action) is ready to use [fragment operation](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment-operation) 
created by an [action factory](#action-factory). Action may be parametrized with JSON (`config`). 
You can use actions' factories to create actions, however, we recommend using the [action provider](#action-provider) 
that would hide the complexity of initialization (maintaining stateful actions, combining actions 
with their behaviours).

### Action
Action is a single [fragment operation](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment-operation) 
that operates on a [Fragment](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment).
Action can invoke an API and save the response body in a fragment payload or simply modify a fragment's body.

Action may be decorated with [behaviours](#behaviours). Actions deployed on the [Vert.x Event Bus](https://vertx.io/docs/vertx-core/java/#event_bus) are called [Knots](#knot).

Please note that actions can be **stateless** and **stateful**.

Below there is a list of core actions:
- [HTTP Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#http-action) - call an external Web API and store the data in a fragment's payload
- [Inline Body Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#inline-body-action) - replaces a fragment's body with the new one
- [Inline Payload Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#inline-payload-action) - adds some data into a fragment's payload
- [Payload To Body Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#payload-to-body-action) - rewrite a fragment's payload to the body

### Knot
**Knot** is a scalable **Action** that is available on the [Vert.x Event Bus](https://vertx.io/docs/vertx-core/java/#event_bus). 
See the [Template Engine](https://github.com/Knotx/knotx-template-engine/blob/master/core/src/main/java/io/knotx/te/core/TemplateEngineKnot.java) 
module as an example.

### Behaviours
Behaviour is a special action that wraps the original action and adds some functionality e.g 
employ the circuit breaker pattern or add a cache for data.

Below there is a list of core behaviours:
- [Circuit Breaker Behaviour](https://github.com/Knotx/knotx-fragments/tree/master/action/core#circuit-breaker-behaviour) - it is a kind of quarantine for actions, it use the [Vert.x Circuit Breaker](https://vertx.io/docs/vertx-circuit-breaker/java/) implementation
- [In-memory Cache Behaviour](https://github.com/Knotx/knotx-fragments/tree/master/action/core#in-memory-cache-behaviour) - caches a fragment's payload to reduce number of action invocations

### Action provider
Action provider initializes actions, combines actions with behaviours and caches stateful ones. It
uses aliases to identify a particular action. Each alias points to an action factory name and therefore its configuration.
If action is a behaviour, it also points to a wrapped action (by alias as well).
 
Action factories register inside the provider by a [Java Service Loader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
 
Once configured, the action provider provides actions by alias e.g.
```hocon
Action actionInstance = provider.get(ACTION_ALIAS);
```

Action provider is initialized with an alias to [action factory options](#action-factory-options) map. 
See the sample configuration below:
```hocon
actions {
  ALIAS_A: ACTION_FACTORY_OPTIONS_A
  ALIAS_B: ACTION_FACTORY_OPTIONS_B
}
```

#### Action factory options
Action factory options contain:
- [action factory](#action-factory) name
- action JSON configuration
- `doAction` for behaviours

and allows:
- connecting a action factory with configuration
- linking actions with behaviours.

More details [here](https://github.com/Knotx/knotx-fragments/blob/master/action/core/docs/asciidoc/dataobjects.adoc#actionfactoryoptions).

### Action factory
Action [factory](https://github.com/Knotx/knotx-fragments/blob/master/handler/api/src/main/java/io/knotx/fragments/handler/api/ActionFactory.java)
creates an action instance providing an [action configuration](#action-configuration). It is identified by name (a simple text value).

See the full list of available action factories [here](https://github.com/Knotx/knotx-fragments/tree/master/action/core).

#### Action configuration
Action configuration is a simple JSON object containing action specific entries.

See the [Core module documentation](https://github.com/Knotx/knotx-fragments/tree/master/action/core) 
for a full list of [actions](#action).