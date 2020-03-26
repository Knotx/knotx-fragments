# Fragment HTML Body Writer
It wraps the beginning and the end of configured fragments with HTML comments and adds the `script` tag
with details of fragment processing:
```
<!-- data-knotx-id="FRAGMENT_IDENTIFIER" -->
  <script data-knotx-id="FRAGMENT_IDENTIFIER" type="application/json">
    FRAGMENT_EXECUTION_LOG
  </script>
  FRAGMENT_BODY
<!-- data-knotx-id="FRAGMENT_IDENTIFIER" -->
```
where:
  - `FRAGMENT_IDENTIFIER` is a unique identifier of a fragment, randomly generated value on the 
  HTTP request
  - `FRAGMENT_EXECUTION_LOG` is a JSON, which contains fragment processing details

`FRAGMENT_EXECUTION_LOG` is described [here](https://github.com/Knotx/knotx-fragments/tree/master/handler/consumer#what-is-the-execution-log).

## How to start?
- Configure consumer in handler
  - add a [consumer factory configuration](#how-to-configure) to the [Fragments Handler options](https://github.com/Knotx/knotx-fragments/blob/master/handler/core/docs/asciidoc/dataobjects.adoc#fragmentshandleroptions)
  - set `logLevel` to `INFO` for more fragments' processing details in the [Default Task Factory config](https://github.com/Knotx/knotx-fragments/blob/master/handler/core/docs/asciidoc/dataobjects.adoc#defaulttaskfactoryconfig)
- Visualize fragments with [Knot.x Fragments Chrome Extension](https://github.com/Knotx/knotx-fragments-chrome-extension) 
  or simply check the HTML markup
  
Any issues? Please check the [functional](https://github.com/Knotx/knotx-stack/blob/master/src/functionalTest/java/io/knotx/stack/functional/KnotxFragmentsDebugDataWithHandlebarsTest.java) test configuration.

## How to configure?
It must be configured in `consumerFactories`
```hocon
consumerFactories = [
  {
    factory = fragmentHtmlBodyWriter
    config { CONSUMER_CONFIG }
  }
]
```
where `CONSUMER_CONFIG` consists of:
```hocon
condition {
  param = debug
  # header = x-knotx-debug
}
fragmentTypes = [ "snippet" ]
```
It runs when any of the following conditions are met:
 - `param` - an original request contains a parameter with the *given name* (e.g. by configuring 
 `param=debug`, requesting `{address}?debug=true` will meet the condition),
 - `header` - condition is analogous, but the value comes from the request header.
 
If no condition is configured, the consumer is not triggered.