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
package io.knotx.fragments.handler.consumer.html;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

class GraphNodeOperationLogTest {

  @Test
  void validateEmpty() {
    // given
    GraphNodeOperationLog log = GraphNodeOperationLog.empty();

    // when
    GraphNodeOperationLog result = new GraphNodeOperationLog(log.toJson());

    // then
    assertTrue(result.getFactory().isEmpty());
    assertTrue(result.getData().isEmpty());
  }

  @Test
  void validateSerialization() {
    // given
    String factory = "factory";
    JsonObject data = new JsonObject().put("key", "value");

    GraphNodeOperationLog log = GraphNodeOperationLog.newInstance(factory, data);

    // when
    GraphNodeOperationLog result = new GraphNodeOperationLog(log.toJson());

    // then
    assertEquals(factory, result.getFactory());
    assertEquals(data, result.getData());
  }
}