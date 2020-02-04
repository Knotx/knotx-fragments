/*
 * Copyright (C) 2019 Knot.x Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.knotx.fragments.handler.action.http;

import io.knotx.fragments.handler.action.http.options.HttpActionOptions;
import io.knotx.fragments.handler.action.http.request.EndpointRequest;
import io.knotx.fragments.handler.action.http.request.ResponsePredicatesProvider;
import io.reactivex.Single;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ErrorConverter;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import java.util.Set;

class EndpointInvoker {

  private static final ResponsePredicate IS_JSON_RESPONSE = createJsonPredicate();
  private static final String JSON = "JSON";

  private final WebClient webClient;
  private final HttpActionOptions httpActionOptions;
  private final ResponsePredicatesProvider predicatesProvider = new ResponsePredicatesProvider();
  private final boolean isJsonPredicate;

  EndpointInvoker(WebClient webClient, HttpActionOptions httpActionOptions) {
    this.webClient = webClient;
    this.httpActionOptions = httpActionOptions;
    this.isJsonPredicate = httpActionOptions.getResponseOptions().getPredicates().contains(JSON);
  }

  Single<HttpResponse<Buffer>> invokeEndpoint(EndpointRequest request) {
    return Single.just(request)
        .map(this::createHttpRequest)
        .doOnSuccess(this::addPredicates)
        .flatMap(HttpRequest::rxSend);
  }

  private HttpRequest<Buffer> createHttpRequest(EndpointRequest endpointRequest) {
    return webClient
        .request(HttpMethod.GET,
            httpActionOptions.getEndpointOptions().getPort(),
            httpActionOptions.getEndpointOptions().getDomain(),
            endpointRequest.getPath())
        .timeout(httpActionOptions.getRequestTimeoutMs())
        .putHeaders(endpointRequest.getHeaders());
  }

  private void addPredicates(HttpRequest<Buffer> request) {
    if (isJsonPredicate) {
      request.expect(IS_JSON_RESPONSE);
    }
    attachResponsePredicatesToRequest(request,
        httpActionOptions.getResponseOptions().getPredicates());
  }

  private void attachResponsePredicatesToRequest(HttpRequest<Buffer> request,
      Set<String> predicates) {
    predicates.stream()
        .filter(p -> !JSON.equals(p))
        .map(predicatesProvider::fromName)
        .forEach(request::expect);
  }

  private static ResponsePredicate createJsonPredicate() {
    return ResponsePredicate.create(
        ResponsePredicate.JSON,
        ErrorConverter.create(result -> {
          throw new ReplyException(ReplyFailure.RECIPIENT_FAILURE, result.message());
        })
    );
  }

}
