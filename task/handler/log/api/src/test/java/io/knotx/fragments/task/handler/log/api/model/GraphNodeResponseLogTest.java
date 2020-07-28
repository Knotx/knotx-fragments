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
package io.knotx.fragments.task.handler.log.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

class GraphNodeResponseLogTest {

  @Test
  void validateSerialization() {
    // given
    String transition = FragmentResult.SUCCESS_TRANSITION;

    JsonObject log = new JsonObject().put("debug", "true");

    GraphNodeResponseLog origin = GraphNodeResponseLog
        .newInstance(transition, log);

    // when
    GraphNodeResponseLog tested = new GraphNodeResponseLog(origin.toJson());

    // then
    assertEquals(transition, tested.getTransition());
    assertEquals(log, tested.getLog());
  }
}