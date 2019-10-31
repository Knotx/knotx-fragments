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
package io.knotx.fragments.handler.api;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Creates an instance of {@link Action} class.
 */
public interface ActionFactory {

  /**
   * Action factory name.
   *
   * @return action factory name
   */
  String getName();

  /**
   * Creates an instance of {@link Action} class.
   *
   * @param alias - action alias
   * @param config - JSON configuration
   * @param vertx - vertx instance
   * @param doAction action to be applied, if no action should be called in chain then it is
   * <pre>null</pre>
   * @return function to execute
   */
  Action create(String alias, JsonObject config, Vertx vertx, Action doAction);

}
