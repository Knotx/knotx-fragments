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
package io.knotx.fragments.action.library;


import static io.knotx.fragments.action.api.log.ActionLogLevel.fromConfig;
import static io.knotx.fragments.action.library.helper.ValidationHelper.checkArgument;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.api.Cacheable;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

/**
 * Payload Cache Action factory class. It can be initialized with a configuration:
 * <pre>
 *   productDetails {
 *     name = in-memory-cache,
 *     config {
 *       cache {
 *         maximumSize = 1000
 *         ttl = 5000
 *       }
 *       cacheKey = product-{param.id}
 *       payloadKey = product
 *       logLevel = error
 *     }
 *   }
 * </pre>
 */
@Cacheable
public class InMemoryCacheActionFactory implements ActionFactory {

  @Override
  public String getName() {
    return "in-memory-cache";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    final Cache cache = new InMemoryCache(config.getJsonObject("cache"));
    final String payloadKey = getPayloadKey(config);
    final ActionLogLevel logLevel = fromConfig(config, ActionLogLevel.ERROR);

    return new InMemoryCacheAction(cache, payloadKey, logLevel, doAction, config);
  }

  private String getPayloadKey(JsonObject config) {
    String result = config.getString("payloadKey");
    checkArgument(getName(), StringUtils.isBlank(result),
        "Action requires payloadKey value in configuration.");
    return result;
  }
}
