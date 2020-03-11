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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.engine.api.node.NodeType;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GraphNodeExecutionLogTest {

  @Test
  void validateDefaults() {
    // given
    String nodeId = "A";

    GraphNodeExecutionLog executionLog = GraphNodeExecutionLog.newInstance(nodeId);

    // when
    GraphNodeExecutionLog result = new GraphNodeExecutionLog(executionLog.toJson());

    // then
    assertEquals(nodeId, result.getId());
    assertEquals(NodeType.SINGLE, result.getType());
    assertTrue(result.getLabel().isEmpty());
    assertTrue(result.getSubtasks().isEmpty());
    assertTrue(result.getOn().isEmpty());
  }

  @Test
  void validateSerialization() {
    // given
    String nodeId = "A";
    NodeType type = NodeType.COMPOSITE;
    String label = "A Label";

    List<GraphNodeExecutionLog> subtasks = Collections
        .singletonList(GraphNodeExecutionLog.newInstance("AA"));
    GraphNodeOperationLog operation = GraphNodeOperationLog
        .newInstance("factory", new JsonObject());
    Map<String, GraphNodeExecutionLog> on = Collections
        .singletonMap(FragmentResult.SUCCESS_TRANSITION, GraphNodeExecutionLog.newInstance("B"));

    GraphNodeExecutionLog executionLog = GraphNodeExecutionLog
        .newInstance(nodeId, type, label, subtasks, operation, on);

    // when
    GraphNodeExecutionLog result = new GraphNodeExecutionLog(executionLog.toJson());

    // then
    assertEquals(nodeId, result.getId());
    assertEquals(type, result.getType());
    assertEquals(label, result.getLabel());
    assertEquals(subtasks, result.getSubtasks());
    assertEquals(operation, result.getOperation());
    assertEquals(on, result.getOn());
  }

}