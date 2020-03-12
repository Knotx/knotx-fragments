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
package io.knotx.fragments.handler.action.http.request;

import io.knotx.commons.http.request.AllowedHeadersFilter;
import io.knotx.commons.http.request.MultiMapCollector;
import io.knotx.fragments.handler.action.http.options.EndpointOptions;
import io.knotx.server.api.context.ClientRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import java.util.Optional;
import java.util.Set;

class HeadersComposer {

  private final JsonObject additionalHeaders;
  private final Set<String> allowedHeaders;

  HeadersComposer(EndpointOptions endpointOptions) {
    this.additionalHeaders = endpointOptions.getAdditionalHeaders();
    this.allowedHeaders = endpointOptions.getAllowedRequestHeaders();
  }

  MultiMap getRequestHeaders(ClientRequest clientRequest) {
    return Optional.of(clientRequest.getHeaders())
        .map(this::getFilteredHeaders)
        .map(this::addAdditionalHeaders)
        .orElseGet(MultiMap::caseInsensitiveMultiMap);
  }

  void setJsonContentTypeIfEmpty(MultiMap requestHeaders) {
    if(!requestHeaders.contains(HttpHeaderNames.CONTENT_TYPE)) {
      requestHeaders.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
    }
  }

  private MultiMap getFilteredHeaders(MultiMap headers) {
    return headers.names().stream()
        .filter(AllowedHeadersFilter.CaseInsensitive.create(allowedHeaders))
        .collect(MultiMapCollector.toMultiMap(o -> o, headers::getAll));
  }

  private MultiMap addAdditionalHeaders(MultiMap headers) {
    if (additionalHeaders != null) {
      additionalHeaders.forEach(entry -> headers.add(entry.getKey(), entry.getValue().toString()));
    }
    return headers;
  }
}
