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

   * @param config - Action configuration
   * @param vertx - vertx instance
   * <pre>null</pre>
   * @return function to execute
   */
  Action create(ActionConfig config, Vertx vertx);

}
