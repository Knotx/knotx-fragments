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
package io.knotx.fragments.action.library.cache;

import static io.knotx.fragments.action.library.cache.CacheTestUtils.ACTION_ALIAS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.Cacheable;
import io.knotx.fragments.action.api.SyncAction;
import io.knotx.fragments.action.library.exception.ActionConfigurationException;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CacheActionFactoryTest {

  private static final Action IDLE_DO_ACTION = (SyncAction) context -> FragmentResult
      .success(new Fragment("", new JsonObject(), ""));

  private CacheActionFactory tested;

  @BeforeEach
  void setUp() {
    tested = new CacheActionFactory();
  }

  @Test
  @DisplayName("Expect exception when CacheFactory of requested type not found via SPI")
  void notFoundCacheFactory() {
    assertThrows(ActionConfigurationException.class,
        () -> tested.create(ACTION_ALIAS, validConfig("non-existing"), null, IDLE_DO_ACTION));
  }

  @Test
  @DisplayName("Expect exception is propagated when thrown by CacheFactory")
  void invalidCacheConfigThrows() {
    assertThrows(IllegalArgumentException.class, () ->
        tested.create(ACTION_ALIAS, invalidCacheConfig(), null, IDLE_DO_ACTION));
  }

  @Test
  @DisplayName("Expect exception is propagated when thrown by CacheAction constructor")
  void invalidActionConfigThrows() {
    assertThrows(IllegalArgumentException.class, () ->
        tested.create(ACTION_ALIAS, invalidActionConfig(), null, IDLE_DO_ACTION));
  }

  @Test
  @DisplayName("Expect CacheAction returned when config is valid")
  void validConfigProducesValidAction() {
    Action action = tested.create(ACTION_ALIAS, validConfig("empty-cache"), null, IDLE_DO_ACTION);

    assertTrue(action instanceof CacheAction);
  }

  @Test
  @DisplayName("Expect factory to have Cacheable annotation")
  void factoryCacheable() {
    assertTrue(tested.getClass().isAnnotationPresent(Cacheable.class));
  }

  private JsonObject validConfig(String type) {
    return new CacheActionOptions(new JsonObject())
        .setType(type)
        .setPayloadKey("payloadKey")
        .setCacheKey("cacheKey")
        .toJson();
  }

  private JsonObject invalidActionConfig() {
    return new CacheActionOptions(new JsonObject())
        .setType("empty-cache")
        .toJson();
  }

  private JsonObject invalidCacheConfig() {
    return new CacheActionOptions(new JsonObject())
        .setType("empty-cache")
        .setPayloadKey("payloadKey")
        .setCacheKey("cacheKey")
        .setCache(new JsonObject()
            .put("invalidOption", true))
        .toJson();
  }

}
