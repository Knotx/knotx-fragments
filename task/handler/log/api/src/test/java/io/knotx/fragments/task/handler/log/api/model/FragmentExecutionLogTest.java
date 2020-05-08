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
package io.knotx.fragments.handler.consumer.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.consumer.api.model.FragmentExecutionLog.ExecutionStatus;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

class FragmentExecutionLogTest {

  @Test
  void validateSerialization() {
    // given
    Fragment fragment = new Fragment("snippet", new JsonObject(), "body");
    ExecutionStatus status = ExecutionStatus.SUCCESS;
    int startTime = 123;
    int finishTime = 456;

    FragmentExecutionLog executionLog = FragmentExecutionLog.newInstance(fragment, status,
        startTime, finishTime);

    // when
    FragmentExecutionLog result = new FragmentExecutionLog(executionLog.toJson());

    // then
    assertEquals(fragment, result.getFragment());
    assertEquals(status, result.getStatus());
    assertEquals(startTime, result.getStartTime());
    assertEquals(finishTime, result.getFinishTime());
    assertNull(result.getGraph());
  }

  @Test
  void validateSerializationWithGraph() {
    // given
    Fragment fragment = new Fragment("snippet", new JsonObject(), "body");
    ExecutionStatus status = ExecutionStatus.SUCCESS;
    GraphNodeExecutionLog graphLog = GraphNodeExecutionLog.newInstance("id");
    int startTime = 123;
    int finishTime = 456;

    FragmentExecutionLog executionLog = FragmentExecutionLog
        .newInstance(fragment, status, startTime, finishTime, graphLog);

    // when
    FragmentExecutionLog result = new FragmentExecutionLog(executionLog.toJson());

    // then
    assertEquals(fragment, result.getFragment());
    assertEquals(status, result.getStatus());
    assertEquals(startTime, result.getStartTime());
    assertEquals(finishTime, result.getFinishTime());
    assertEquals(graphLog, result.getGraph());
  }

}