# Fragments Actions
It is a tool-kit for integrating with different data sources (e.g. HTTP-based APIs). It 
implements stability patterns to prevent/handle network issues.

## How to start?
- add `knotx-fragments-action-core.X.Y.Z.jar` to your dependencies
- configure the [action](#action), e.g. call the HTTP endpoint with the circuit breaker pattern implemented:
  ```hocon
  fetch-user-with-cb {
    factory = cb
    config {
      circuitBreakerName = delivery-cb
      circuitBreakerOptions {
        maxRetries = 2
        timeout = 500
      }
    }
    doAction = fetch-user
  }
  
  fetch-user {
    factory = http
    config.endpointOptions {
      path = "/service/user/{params.id}"
      domain = localhost
      port = 1024
      allowedRequestHeaders = ["Content-Type"]
    }
  }  
  ```
- retrieve the [action provider](#action-provider) to and invoke the action e.g.
  ```java
  provider.get("fetch-user-with-cb").apply(fragmentContext, resultHandler)
  ```
 
See the [Core module documentation](https://github.com/Knotx/knotx-fragments/tree/master/action/core) 
for a full list of [actions](#action).

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

Action may be decorated with [behaviours](https://github.com/Knotx/knotx-fragments/tree/master/actions/core#behaviours). 
Behaviour is a special action that wraps the original action and adds some functionality e.g 
employ the circuit breaker pattern. 

Please note that actions can be **stateless** and **stateful**.

### Knot
**Knot** is a scalable **Action** that is available on the [Vert.x Event Bus](https://vertx.io/docs/vertx-core/java/#event_bus). 
See the [Template Engine](https://github.com/Knotx/knotx-template-engine/blob/master/core/src/main/java/io/knotx/te/core/TemplateEngineKnot.java) 
module as an example.

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

### Action factory options
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
