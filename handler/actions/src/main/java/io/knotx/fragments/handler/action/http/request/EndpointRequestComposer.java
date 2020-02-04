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
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.common.placeholders.PlaceholdersResolver;
import io.knotx.server.common.placeholders.SourceDefinitions;
import io.vertx.reactivex.core.MultiMap;
import java.util.List;
import java.util.regex.Pattern;

public class EndpointRequestComposer {

  private static final String PLACEHOLDER_PREFIX_PAYLOAD = "payload";
  private static final String PLACEHOLDER_PREFIX_CONFIG = "config";

  private EndpointOptions endpointOptions;

  public EndpointRequestComposer(EndpointOptions endpointOptions) {
    this.endpointOptions = endpointOptions;
  }

  public EndpointRequest createEndpointRequest(FragmentContext context) {
    ClientRequest clientRequest = context.getClientRequest();
    SourceDefinitions sourceDefinitions = buildSourceDefinitions(context, clientRequest);
    String path = PlaceholdersResolver.resolve(endpointOptions.getPath(), sourceDefinitions);
    MultiMap requestHeaders = getRequestHeaders(clientRequest);
    return new EndpointRequest(path, requestHeaders);
  }

  private SourceDefinitions buildSourceDefinitions(FragmentContext context,
      ClientRequest clientRequest) {
    return SourceDefinitions.builder()
        .addClientRequestSource(clientRequest)
        .addJsonObjectSource(context.getFragment()
            .getPayload(), PLACEHOLDER_PREFIX_PAYLOAD)
        .addJsonObjectSource(context.getFragment()
            .getConfiguration(), PLACEHOLDER_PREFIX_CONFIG)
        .build();
  }

  private MultiMap getRequestHeaders(ClientRequest clientRequest) {
    MultiMap filteredHeaders = getFilteredHeaders(clientRequest.getHeaders(),
        endpointOptions.getAllowedRequestHeadersPatterns());
    if (endpointOptions.getAdditionalHeaders() != null) {
      endpointOptions.getAdditionalHeaders()
          .forEach(entry -> filteredHeaders.add(entry.getKey(), entry.getValue().toString()));
    }
    return filteredHeaders;
  }

  private MultiMap getFilteredHeaders(MultiMap headers, List<Pattern> allowedHeaders) {
    return headers.names().stream()
        .filter(AllowedHeadersFilter.create(allowedHeaders))
        .collect(MultiMapCollector.toMultiMap(o -> o, headers::getAll));
  }

}
