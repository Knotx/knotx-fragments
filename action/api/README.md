# Action API

## Action
[Action](https://github.com/Knotx/knotx-fragments/blob/master/action/api/src/main/java/io/knotx/fragments/action/api/Action.java) 
is a single [fragment operation](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment-operation) 
that operates on a [Fragment](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment).

## Knot
Knot is a scalable [action](#action) that is available on the [Vert.x Event Bus](https://vertx.io/docs/vertx-core/java/#event_bus). 
See the [Template Engine](https://github.com/Knotx/knotx-template-engine/blob/master/core/src/main/java/io/knotx/te/core/TemplateEngineKnot.java) 
module as an example.

## Action Log
Actions provide the custom log syntax. See the [ActionLog](https://github.com/Knotx/knotx-fragments/blob/master/action/api/docs/asciidoc/dataobjects.adoc#actionlog) for more details. 

## Action Factory
Action [factory](https://github.com/Knotx/knotx-fragments/blob/master/action/api/src/main/java/io/knotx/fragments/action/api/ActionFactory.java)
creates an action instance providing an action configuration. It is identified by name (a simple text value).

Action configuration is a simple JSON object containing action specific entries.

## @Cacheable
Actions are stateless or stateful. The @Cacheable annotation flags [action factories](#action-factory) 
that produce stateful actions. Actions are cached by their names.