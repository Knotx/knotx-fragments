# Fragments Actions Core

## HTTP Action

HTTP Action is an [Action](https://github.com/Knotx/knotx-fragments/tree/master/handler/api#action) 
that connects to the external WEB endpoint that responds with JSON and saves the response into the 
[Fragment's](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api) `payload`.

### How does it work
HTTP Action uses HTTP Client to connect to the external WEB endpoint. It expects HTTP response with
JSON body. To configure endpoint you will have to provide `domain` and `port` and additionally
a request `path` to the WEB endpoint. The `path` parameter could contain placeholders that will be
resolved with the logic described above.
After the result from the external WEB endpoint is obtained, it is merged with processed
[Fragment's](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api) `payload`
and returned in the [`FragmentResult`](https://github.com/Knotx/knotx-fragments/blob/master/handler/api/docs/asciidoc/dataobjects.adoc#FragmentResult)
together with Transition.

##### Parametrized services calls
Http Action supports request parameters, `@payload` and `@configuration` resolving for the `path` parameter which defines the final request URI.
Read more about placeholders in the [Knot.x Server Common Placeholders](https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders#available-request-placeholders-support).

The `@payload` and `@configuration` are values stored in [Fragment](https://github.com/Knotx/knotx-fragments/tree/master/api#knotx-fragment-api).
For this structures use corresponding prefixes: `payload` and `config` 

### How to use
Define HTTP Action using `http` factory and providing configs `endpointOptions` and `responseOptions` in the Fragment's Handler
`actions` section.

```hocon
actions {
  book {
    factory = http
    config {
      endpointOptions {
        path = /service/mock/book.json
        domain = localhost
        port = 3000
        allowedRequestHeaders = ["Content-Type"]
      }
      responseOptions {
        predicates = [JSON]
        forceJson = false
      }       
    }
  }
}
```

- `endpointOptions` config describes quite basic request parameters as `path`, `domain`, `port` and `allowedRequestHeaders` which
are necessary to perform http request 
- `responseOptions` config is responsible for handling incoming request properly. Here we can specify `predicates` - it's array
containing Vert.x response predicates, to get more familiar with it, please visit [this page](https://vertx.io/blog/http-response-validation-with-the-vert-x-web-client/).
You may find all available predicates [here](https://vertx.io/docs/apidocs/io/vertx/ext/web/client/predicate/ResponsePredicate.html).
Providing `JSON` predicate causes `Content-Type` check - when `Content-Type` won't be equal to `application/json` we'll get
error transition. We can also specify `forceJson` param. When `Content-Type` won't be equal to `application/json` and `forceJson`
is true, response will be processed as json. If it won't be json, request ends with error transition.

Table below shows the behaviour of HttpAction depending on provided `responseOptions` config and response:

| Content-Type     | forceJSON | JSON predicate | Body | Transition | Response |
| ---------------- |:---------:| --------------:|  ---:|  ---------:| --------:|
| application/json | false     | -              | JSON | _success   | JSON     |
| application/text | true      | -              | JSON | _success   | JSON     |
| application/json | false     | JSON           | JSON | _success   | JSON     |
| application/text | false     | -              | JSON | _success   | text     |
| application/text | false     | -              | RAW  | _success   | text     |
| application/json | false     | -              | RAW  | _error     | -        |
| application/text | true      | -              | RAW  | _error     | -        |
| application/json | false     | JSON           | RAW  | _error     | -        |
| application/text | false     | JSON           | JSON | _error     | -        |
| application/text | true      | JSON           | JSON | _error     | -        |

#### Node log
HTTP Action adds details about the request, response and occurred errors to [node log](https://github.com/Knotx/knotx-fragments/tree/master/engine#node-log). 
If the log level is `ERROR`, then only failing situations are logged: exception occurs during processing, response predicate is not valid, or status code is between 400 and 600. 
For the `INFO` log level all items are logged.

Node log is presented as `JSON` and has the following structure:
```json
{
  "request": REQUEST_DATA,
  "response": RESPONSE_DATA,
  "responseBody": RESPONSE_BODY,
  "errors": LIST_OF_ERRORS
}
```
`REQUEST_DATA` contains the following entries:
 ```json
{
  "path": "/api/endpoint",
  "requestHeaders": {
    "Content-Type": "application/json"
  },
  "requestBody": ""
}
```
`RESPONSE_DATA` contains the following entries:
```json
{
  "httpVersion": "HTTP_1_1",
  "statusCode": "200",
  "statusMessage": "OK",
  "headers": {
    "Content-Type": "application/json"
  },
  "trailers": {
  },
  "httpMethod": "GET",
  "requestPath": "http://localhost/api/endpoint"
}
```
`RESPONSE_BODY` contains the response received from service, it's text value.

`LIST_OF_ERRORS` looks like the following:
```json
[
  {
    "className": "io.vertx.core.eventbus.ReplyException",
    "message": "Expect content type application/text to be one of application/json"
  },
  {
    "className": "some other exception...",
    "message": "some other message..."
  }
]
```
The table below presents expected entries in node log on particular log levels depending on service response:

| Response                                   | Log level  | Log entries   |
| ------------------------------------------ | ---------- | ------------------------------------------ |
| `_success`                                 | INFO       | REQUEST_DATA, RESPONSE_DATA, RESPONSE_BODY |
| `_success`                                 | ERROR      |                                            |
| exception occurs and `_error`              | INFO       | REQUEST_DATA, LIST_OF_ERRORS               |
| exception occurs and `_error`              | ERROR      | REQUEST_DATA, LIST_OF_ERRORS               |
| `_error` (e.g service responds with `500`) | INFO       | REQUEST_DATA, RESPONSE_DATA, RESPONSE_BODY |
| `_error` (e.g service responds with `500`) | INFO       | REQUEST_DATA, RESPONSE_DATA                |

#### Supported HTTP methods

Currently Knot.x supports `GET`, `POST`, `PUT`, `PATCH`, `DELETE` and `HEAD` HTTP methods.
This is specified using `httpMethod` option under `config` node of HttpAction (defaults to `GET`).

For `POST`, `PUT` and `PATCH` methods, request body is sent. 
Body can be specified either as String or as a JSON, using `body` and `bodyJson` parameters under `endpointOptions`.
It is possible to perform interpolation using values from the original request, Fragment's configuration or Fragment's payload.
For details, see [Knot.x Server Common Placeholders](https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders#available-request-placeholders-support).
Please note that unlike for path, placeholders in body will not be URL-encoded.
 
To enable body interpolation, set `interpolateBody` flag under `endpointOptions` to `true`.

#### Encoding

Currently, only placeholders in a path are URL-encoded. Any other value is passed as-is, i.e.:
- path schema
- body schema + resolved placeholders
- custom header
- client's header allowed to be passed on

Therefore, all configuration-based values (path schema, body schema, headers) **must** be encoded in the configuration.

Values used for path interpolation **must not** be encoded (otherwise a double encoding might occur).

Client request's values used for interpolation are already in a raw form (when decoded by Knot.x HTTP Server).

#### Detailed configuration
All configuration options are explained in details in the [Config Options Cheetsheet](https://github.com/Knotx/knotx-fragments/tree/master/handler/core/docs/asciidoc/dataobjects.adoc).


## Inline Body Action
Inline Body Action replaces Fragment body with specified one. Its configuration looks like:
```hocon
factory = "inline-body"
config {
  body = <div>Product not available at the moment</div>
  logLevel = error
}
```
The default `body` value is empty content.

### Inline Body Action Log

When `loglevel` is set to `info`, action is getting logged:
- `originalBody` - the incoming Fragment's body
- `body` - new Fragment body

## Inline Payload Action
Inline Payload Action puts JSON / JSON Array in Fragment payload with a specified key (alias). Its 
configuration looks like:
```hocon
factory = "inline-payload"
config {
  alias = product
  payload {
    productId = 1234
    description = "some description"
  }
  # payload = [
  #   "first product", "second product"
  # ]
}
```
The default `alias` is action alias.

## Inline Payload Action Log

When `loglevel` is set to `info`, the action is getting logged:
- `key` - payload key
- `value` - payload value

## Payload To Body Action
Payload To Body Action copies to Fragment body specified payload key value. Its configuration looks like:
```hocon
factory = payload-to-body
config {
  key = "some payload key"
}
```
If no key is specified the whole payload will be copied. A key can direct nested values. For example 
for the payload:
```hocon
someKey {
  someNestedKey {
    attr1 = value1
    attr2 = value2 
  }
}
```
and key value `someKey.someNestedKey` body value will look like:
```hocon
{ 
  attr1 = value1
  attr2 = value2 
}
```

# Behaviours
Behaviours wrap other behaviours or [actions](https://github.com/Knotx/knotx-fragments/blob/master/handler/api#action) and delegate a fragment processing to them. 
They can introduce some stability patterns such as retires, it means that they can call a wrapped 
action many times.

## Circuit Breaker Behaviour
It wraps a simple action with the [Circuit Breaker implementation from Vert.x](https://vertx.io/docs/vertx-circuit-breaker/java/).
Its configuration looks like:
```hocon
factory = "cb"
config {
  circuitBreakerName = product-cb-name
  circuitBreakerOptions {
    # number of failure before opening the circuit
    maxFailures = 3
    # consider a failure if the operation does not succeed in time
    timeout = 2000
    # time spent in open state before attempting to re-try
    resetTimeout = 10000
  }
  # transitions from doAction that mean error
  errorTransitions = [ _error, custom ]
  logLevel = INFO
}
doAction = product
```
The `doAction` attribute specifies a wrapped simple action by its name. When `doAction` throws an 
error or times out then the custom `_fallback` transition is returned.

| #1 `doAction` result  | Retry `doAction` result | CB result (Transition, Log)  |
| :-------------------: |:----------------------:|:-------------------------|
| transition: `_success`| -                      |  `_success`, [s]         |
| transition: `_error`  | transition: `_success` | `_success`, [e,s]        |
| Failure               | transition: `_success` | `_success`, [e,s]        |
| Failure               | transition: `_error`   | `_fallback`, [e,e]       |
| Failure               | TIMEOUT                |  `_fallback`, [e,t]      |
| TIMEOUT               | Failure                |  `_fallback`, [t,e]      |
| TIMEOUT               | TIMEOUT                | `_fallback`, [t,t]       |
| custom transition     | -                      |  `_success`, [s]         |
| custom transition that means error  | custom transition that means error | `_error`, [e,e]        |

Labels:
- TIMEOUT - `doAction` does not end withing the required time (`circuitBreakerOptions.timeout`), 
- please note that `doAction` is not interrupted by a circuit breaker
- Failure - `doAction` fails, means that `doAction` calls `failed` method on result handler
- Exception - `doAction` throws an exception
- `s` - success, `e` - error, `t` - timeout

### Circuit Breaker Action Log

Circuit Breaker logs the following data

 - `invocationCount` - number of retries
 - `error` - contains exception details when: 
    - `doAction` fails, 
    - `doAction` ends with `_error` transition,
    - CB times out `doAction` invocation.

Circuit Breaker log includes logs produced by the [`doAction`](#circuit-breaker-behaviour). Each 
`invocation log` has entries:

 - `duration` - how long takes execution of action - in milisecond
 - `succuess` - execution ends up with success - ()
 - `actionLog` - Wrapped action log
 
Please note that not every call can be visible in `invocation log` entry.  


| Result                 | Invocation log  |
| :--------------------: |:-----|
| transition: `_success` |  Yes |
| transition: `_error`   |  Yes |
| TIMEOUT                |  No  |
| Failure                |  No  |
| Exception              |  No  |

## In-memory Cache Behaviour
It wraps a simple action with cache. It caches a payload values added by a `doAction` action and 
puts cached values in next invocations. It uses in-memory Guava cache implementation. The 
configuration looks like:
```hocon
factory = "in-memory-cache"
config {
  cache {
    maximumSize = 1000
    # in milliseconds
    ttl = 5000
  }
  cacheKey = "product-{param.id}"
  payloadKey = product
  logLevel = error
}
doAction = product-cb
```
Please note that cacheKey can be parametrized with request data like params, headers etc. Read 
[Knot.x HTTP Server Common Placeholders](https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders)
documentation for more details.

### In-Memory Cache Action Log

In-Memory Cache logs most activities when `logLevel` option is set to `info`.

In-Memory Cache logs the following events:

 - `cache_hit` - occurs when there is an associated value in the cache
    - `cache_key`
    - `cached_value`
 - `cache_miss` - occurs when there is no associated value in the cache, `doAction` returns with successful transition and a payload that can be cached
    - `cache_key`
    - `computed_value`
 - `cache_pass` - occurs when there is no associated value in the cache and `doAction` returns with no cacheable data or an error transition. In either case, this event gets logged on `error` log level. 
    - `cache_key`

In-Memory Cache log includes logs produced by the `doAction`. Each 
`invocation log` has entries:

 - `duration` - how long takes execution of action - in milisecond
 - `succuess` - execution ends up with success - ()
 - `actionLog` - Wrapped action log
 
Please note that not every call can be visible in `invocation log` entry.  


| Result                 | Invocation log  |
| :--------------------: |:-----|
| transition: `_success` |  Yes (info level) |
| transition: `_error`   |  Yes (error level) |
| TIMEOUT                |  No  |
| Failure                |  No  |
| Exception              |  No  |
