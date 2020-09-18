# Action Library
It is a core [actions](https://github.com/Knotx/knotx-fragments/tree/master/action#action) and [behaviours](https://github.com/Knotx/knotx-fragments/tree/master/action#behaviour) 
library.

## Action
It is a list of core actions:
- [HTTP Action](#http-action) - invokes a web API and stores a response body in a fragment's payload
- [Inline Body Action](#inline-body-action) - replaces a fragment's body with the configured value
- [Inline Payload Action](#inline-payload-action) - adds a JSON data into a fragment's payload
- [CopyPayloadKeyActionFactory](#copy-payload-key-action) - copies data inside Fragment's payload
- [Payload To Body Action](https://github.com/Knotx/knotx-fragments/tree/master/action/library#payload-to-body-action) - rewrites a fragment's payload to the body

### HTTP Action
HTTP Action calls a Web API (e.g. RESTful API), decodes the response body (e.g. text to JSON object) and finally stores all response details in a fragment's 
payload. It [supports](#supported-http-methods) `GET`, `POST`, `PUT`, `PATCH`, `DELETE` and `HEAD` HTTP methods.

#### How does it work
It uses [Vert.x Web Client](https://vertx.io/docs/vertx-web-client/java/), an asynchronous HTTP and HTTP/2 client, to call web APIs and handle responses. All 
request/response interaction aspects are fully configurable:
- web API domain, port and path
- response body decoding (plain String, Json object or Json Array)
- success criteria (it uses [Vert.x Web Client response predicates](https://vertx.io/docs/vertx-web-client/java/#response-predicates))

The sample configuration for fetching resources (`/products`) from REST API:
```
fetch-products {
  factory = http
  config {
    httpMethod = GET # this is the default so can be skipped
    endpointOptions {
      path = /products
      domain = localhost
      port = 8080
      allowedRequestHeaders = ["Content-Type"]
    }
    responseOptions {
      predicates = [SC_SUCCESS, JSON]
      forceJson = false
    }
  }
}
```

The action does the HTTP `GET` request to `http://localhost:8080/products`. By default, a Vert.x Web Client request ends with an error only if something wrong 
happens at the network level. The response options (success conditions) indicate that we expect:
- `2xx` response status code
- `content-type` response header is `application.json`
- response body is a json object or json array

Please note that all client (`4xx`) and server (`5xx`) error response status codes fail the action.  

For the above example, when all processing ends with success, and the response body contains:
```
[
  {
    "id": "sku-01",
    "name": "Vert.x in Action"
  },
  {
    "id": "sku-02",
    "name": "Knot.x in Action"
  }
]
```

the action fragment result will contain:
- transition is `_success`
- fragment's payload with `_request`, `_response` and `_result` details (under the action alias/name (`fetch-products`) key):
```
"fetch-products": {
  "_request": {
    "type": "HTTP",
    "source": "/products",
    "metadata": {
      "headers": {}
    }
  },
  "_response": {
    "success": true,
    "metadata": {
      "statusCode": "200",
      "headers": {
        "Content-Type": [
          "application/json; charset=UTF-8"
        ],
        "Transfer-Encoding": [
          "chunked"
        ],
        "Server": [
          "Jetty(9.2.24.v20180105)"
        ]
      }
    }
  },
  "_result": [
    {
      "id": "sku-01",
      "name": "Vert.x in Action"
    },
    {
      "id": "sku-02",
      "name": "Knot.x in Action"
    }
  ]
}
```

#### Supported HTTP methods
HTTP Action supports `GET`, `POST`, `PUT`, `PATCH`, `DELETE` and `HEAD` HTTP methods.
This is specified using `httpMethod` option (defaults to `GET`).

For `POST`, `PUT` and `PATCH` methods, request body is sent. A body can be specified either as String or as a JSON, using `body` and `bodyJson` parameters 
under `endpointOptions`. 

#### Parametrized options
It is possible to perform interpolation using values from the original **request** and **fragment** (configuration or payload).

The available interpolation values:
- `{param.x}`, `{header.x}`, `{uri.*}`, `{slingUri.*}`, more details [here](https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders#available-request-placeholders-support)
- `{payload.x.y}` - is a fragment's payload with key `x.y`
- `{config.x}` - is a fragment configuration value under the key `x`

##### Endpoint path
All path placeholders are substituted with encoded values according to the RFC standard. 
However, there are two exceptions:
- space character is substituted by `%20` instead of `+`.
- slash character `/` remains as it is.

The sample usage:
```
endpointOptions.path = /users/{payload.fetch-user-info._result.id}
```

##### Request body
Please note that unlike for path, placeholders in body will not be URL-encoded.
To enable body interpolation, set `interpolateBody` flag under `endpointOptions` to `true`.

The sample usage:
```
endpointOptions.bodyJson = {
  user-data-id = "{payload.fetch-user-info._result.id}"
}
endpointOptions.interpolateBody = true
```

#### Success criteria
The `responseOptions` config is responsible for handling responses properly. Here we can specify `predicates` - it's array
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

#### Encoding
Currently, only placeholders in a path are URL-encoded. Any other value is passed as-is, i.e.:
- path schema
- body schema + resolved placeholders
- custom header
- client's header allowed to be passed on

Therefore, all configuration-based values (path schema, body schema, headers) **must** be encoded in the configuration.

Values used for path interpolation **must not** be encoded (otherwise a double encoding might occur).

Client request's values used for interpolation are already in a raw form (when decoded by Knot.x HTTP Server).


### Inline Body Action
Inline Body Action replaces Fragment body with specified one. Its configuration looks like:
```hocon
factory = "inline-body"
config {
  body = <div>Product not available at the moment</div>
  logLevel = error
}
```
The default `body` value is empty content.

#### Inline Body Action log

When `loglevel` is set to `info`, action is getting logged:
- `originalBody` - the incoming Fragment's body
- `body` - new Fragment body

### Inline Payload Action
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

#### Inline Payload Action log

When `loglevel` is set to `info`, the action is getting logged:
- `key` - payload key
- `value` - payload value

### Copy Payload Key Action
Copy Payload Key Action copies data inside Fragment's payload. Its configuration looks like:
```hocon
factory = copy-payload-key
config {
  from = "some.possibly.nested.origin._result"
  to = "destination.possibly.nested"
}
```
If no data is present in payload under `from` key, the action has no effect on `Fragment`.

### Payload To Body Action
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

## Behaviour
It is a list of core actions:
- [Circuit Breaker Be](#circuit-breaker-behaviour) - wraps an action with circuit breaker
- [In-memory Cache Behaviour](#in-memory-cache-behaviour) - adds cache for wrapped action

### Circuit Breaker Behaviour
It envelopes an action with the [Circuit Breaker implementation from Vert.x](https://vertx.io/docs/vertx-circuit-breaker/java/).
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

#### Circuit Breaker Behaviour log

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

### In-memory Cache Behaviour
It wraps a simple action with cache. It caches a payload values added by a `doAction` action and 
puts cached values in next invocations.

Cache implementation is selected the by `type` option. Knot.x provides OOTB `in-memory` cache factory using Guava cache.
Custom types can be added by implementing `CacheFactory` interface and making it available via Service Provider Interface (just like `ActionFactory` implementations).

The in-memory cache uses Guava cache implementation. The 
configuration looks like:
```hocon
factory = "cache"
config {
  cache {
    maximumSize = 1000
    # in milliseconds
    ttl = 5000
  }
  type = "in-memory"
  cacheKey = "product-{param.id}"
  payloadKey = product
  logLevel = error
}
doAction = product-cb
```
Please note that cacheKey can be parametrized with request data like params, headers etc. Read 
[Knot.x HTTP Server Common Placeholders](https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders)
documentation for more details.

#### Cache Behaviour log

Cache logs most activities when `logLevel` option is set to `info`.

Cache logs the following events:

 - `cache_hit` - occurs when there is an associated value in the cache
    - `cache_key`
    - `cached_value`
 - `cache_miss` - occurs when there is no associated value in the cache, `doAction` returns with successful transition and a payload that can be cached
    - `cache_key`
    - `computed_value`
 - `cache_pass` - occurs when there is no associated value in the cache and `doAction` returns with no cacheable data or an error transition. In either case, this event gets logged on `error` log level. 
    - `cache_key`

Cache log includes logs produced by the `doAction`. Each 
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
