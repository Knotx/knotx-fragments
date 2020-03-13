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
package io.knotx.fragments.handler.consumer.html.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.json.JsonArray;
import org.junit.jupiter.api.Test;

class GraphNodeResponseLogTest {

  @Test
  void validateSerialization() {
    // given
    String transition = FragmentResult.SUCCESS_TRANSITION;

    JsonArray invocations = new JsonArray().add("invocation");

    GraphNodeResponseLog log = GraphNodeResponseLog
        .newInstance(transition, invocations);

    // when
    GraphNodeResponseLog result = new GraphNodeResponseLog(log.toJson());

    // then
    assertEquals(transition, result.getTransition());
    assertEquals(invocations, result.getInvocations());
  }
}