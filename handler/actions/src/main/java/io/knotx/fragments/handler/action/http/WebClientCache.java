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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.ext.web.client.WebClient;

class WebClientCache {

  private static final long WEB_CLIENT_CACHE_SIZE = 200;

  // Cache for WebClient, based on the configuration! (Can be shared between actions)
  private Cache<JsonObject, WebClient> cache = CacheBuilder.newBuilder()
      .maximumSize(WEB_CLIENT_CACHE_SIZE)
      .build();

  public WebClient getOrCreate(Vertx vertx, WebClientOptions webClientOptions) {
    WebClient webClient = cache.getIfPresent(webClientOptions.toJson());
    if (webClient == null) {
      return createAndCache(vertx, webClientOptions);
    } else {
      return webClient;
    }
  }

  private WebClient createAndCache(Vertx vertx, WebClientOptions webClientOptions) {
    WebClient webClient = WebClient
        .create(io.vertx.reactivex.core.Vertx.newInstance(vertx), webClientOptions);
    cache.put(webClientOptions.toJson(), webClient);
    return webClient;
  }

}
