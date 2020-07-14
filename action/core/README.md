# Action Core
This modules provides mechanism to manage (create/reuse) action instances.

## Action provider
Action provider initializes actions, combines actions with behaviours and caches stateful ones. It
uses aliases to identify a particular action. Each alias points to an action factory name and therefore its configuration.
If action is a behaviour, it also points to a wrapped action (by alias as well).
 
Action factories register inside the provider by a [Java Service Loader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
 
Once configured, the action provider provides actions by aliases e.g.
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
- linking action factory with configuration
- wrapping actions with behaviours.

Find more details [here](https://github.com/Knotx/knotx-fragments/blob/master/action/core/docs/asciidoc/dataobjects.adoc#actionfactoryoptions).
