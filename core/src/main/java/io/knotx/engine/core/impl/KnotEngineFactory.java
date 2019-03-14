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
package io.knotx.engine.core.impl;

import io.knotx.engine.core.KnotEngine;
import io.vertx.reactivex.core.Vertx;

/**
 * The Knot Engine factory class hides the details of engine implementation and allows for the
 * delivery of other engine implementations in the future.
 */
public class KnotEngineFactory {

  /**
   * Gets Knot Engine instance.
   *
   * @param vertx - RX Vert.x API wrapper
   */
  public static KnotEngine get(Vertx vertx) {
    return new DefaultKnotEngine(vertx);
  }

}
