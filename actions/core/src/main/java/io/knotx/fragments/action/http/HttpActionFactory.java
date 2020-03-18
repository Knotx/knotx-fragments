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
package io.knotx.fragments.action.http;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.HEAD;
import static io.vertx.core.http.HttpMethod.PATCH;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import com.google.common.collect.ImmutableSet;
import io.knotx.fragments.action.exception.ActionConfigurationException;
import io.knotx.fragments.action.http.options.HttpActionOptions;
import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.api.Cacheable;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.Set;
import java.util.stream.Stream;

@Cacheable
public class HttpActionFactory implements ActionFactory {

  private static final Set<HttpMethod> SUPPORTED_METHODS = ImmutableSet
      .of(GET, POST, PATCH, PUT, DELETE, HEAD);

  private final WebClientCache webClientCache = new WebClientCache();

  @Override
  public String getName() {
    return "http";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    HttpActionOptions options = new HttpActionOptions(config);

    validateNoDoAction(doAction, alias);
    validateHttpMethodSupported(options, alias);

    WebClient webClient = webClientCache.getOrCreate(vertx, options.getWebClientOptions());
    return tryToCreateAction(webClient, options, alias);
  }

  private void validateNoDoAction(Action doAction, String alias) {
    if (doAction != null) {
      throw new ActionConfigurationException(alias, "Http Action can not wrap another action");
    }
  }

  private void validateHttpMethodSupported(HttpActionOptions options, String alias) {
    HttpMethod method = findHttpMethod(options.getHttpMethod(), alias);
    if (!SUPPORTED_METHODS.contains(method)) {
      throw new ActionConfigurationException(alias,
          String.format("HttpMethod %s configured for HttpAction is not supported",
              options.getHttpMethod()));
    }
  }

  private HttpMethod findHttpMethod(String methodName, String actionAlias) {
    return Stream.of(HttpMethod.values())
        .filter(httpMethod -> httpMethod.name().equalsIgnoreCase(methodName))
        .findFirst()
        .orElseThrow(() -> new ActionConfigurationException(actionAlias,
            String.format("HttpMethod %s configured for HttpAction not found in Vert.x library",
                methodName)));
  }

  private Action tryToCreateAction(WebClient webClient, HttpActionOptions options, String alias) {
    try {
      return new HttpAction(webClient, options, alias);
    } catch (IllegalArgumentException cause) {
      throw new ActionConfigurationException(alias, "Creating HttpAction failed", cause);
    }
  }

}
