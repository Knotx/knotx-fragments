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

import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class WebClientCacheTest {

  private WebClientCache webClientCache;

  @BeforeEach
  void setUp() {
    webClientCache = new WebClientCache();
  }

  @Test
  @DisplayName("Should provide different WebClient instance when configuration is different")
  void testDifferentConfigurations(VertxTestContext testContext, Vertx vertx) {
    WebClientOptions optionsA = new WebClientOptions().setSsl(true).setDefaultPort(80);
    WebClientOptions optionsB = new WebClientOptions().setSsl(false).setDefaultPort(8080);

    WebClient webClientA = webClientCache.getOrCreate(vertx, optionsA);
    WebClient webClientB = webClientCache.getOrCreate(vertx, optionsB);

    assertNotEquals(webClientA, webClientB);
    testContext.completeNow();
  }

  @Test
  @DisplayName("Should provide same WebClient instance when configuration is equivalent")
  void testEqualConfigurations(VertxTestContext testContext, Vertx vertx) {
    WebClientOptions optionsA = new WebClientOptions().setSsl(true).setDefaultPort(80);
    WebClientOptions optionsB = new WebClientOptions().setSsl(true).setDefaultPort(80);

    WebClient webClientA = webClientCache.getOrCreate(vertx, optionsA);
    WebClient webClientB = webClientCache.getOrCreate(vertx, optionsB);

    assertEquals(webClientA, webClientB);
    testContext.completeNow();
  }

}
