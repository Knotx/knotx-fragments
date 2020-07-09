# Fragment JSON Consumer
It exposes fragments tasks execution log to JSON response body for further analysis.

## How to start?
- Configure consumer in handler
  - add a [consumer factory configuration](#how-to-configure) to the [Fragments Handler options](https://github.com/Knotx/knotx-fragments/blob/master/handler/core/docs/asciidoc/dataobjects.adoc#fragmentshandleroptions)
  - set `logLevel` to `INFO` for more fragments' processing details in the [Default Task Factory config](https://github.com/Knotx/knotx-fragments/blob/master/handler/core/docs/asciidoc/dataobjects.adoc#defaulttaskfactoryconfig)
  
## How does it work?
When configured, it appends debug data to `Fragment` body under `_knotx_fragment` key, the existing body remains unchanged.
Appended entry will contain data provided by [FragmentExecutionLog](https://github.com/Knotx/knotx-fragments/blob/master/task/handler/log/api/docs/asciidoc/dataobjects.adoc#fragmentexecutionlog).
```
{
  "user" {
    "profile": "admin"
   }
  ...
  "_knotx_fragment": {
      "fragment": {},
      "status": {},
      "graph": {}
  }
}
```

## How to configure?
It must be configured in `consumerFactories`
```hocon
consumerFactories = [
  {
    factory = fragmentJsonBodyWriter
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
fragmentTypes = [ "json" ]
```
It runs when any of the following conditions are met:
 - `param` - an original request contains a parameter with the *given name* (e.g. by configuring 
 `param=debug`, requesting `{address}?debug=true` will meet the condition),
 - `header` - condition is analogous, but the value comes from the request header.
 
If no condition is configured, the consumer is not triggered.
