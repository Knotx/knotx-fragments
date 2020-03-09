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
import static org.junit.jupiter.api.Assertions.assertNull;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

class FragmentExecutionLogTest {

  @Test
  void validateSerialization() {
    // given
    Fragment fragment = new Fragment("snippet", new JsonObject(), "body");
    Status eventStatus = Status.SUCCESS;
    FragmentEvent fragmentEvent = new FragmentEvent(fragment).setStatus(eventStatus);

    FragmentExecutionLog executionLog = FragmentExecutionLog.newInstance(fragmentEvent);

    // when
    FragmentExecutionLog result = new FragmentExecutionLog(executionLog.toJson());

    // then
    assertEquals(eventStatus, result.getStatus());
    assertEquals(fragment, result.getFragment());
    assertEquals(0, result.getStartTime());
    assertEquals(0, result.getFinishTime());
    assertNull(result.getGraph());
  }

  @Test
  void validateSerializationWithGraph() {
    // given
    Fragment fragment = new Fragment("snippet", new JsonObject(), "body");
    Status eventStatus = Status.SUCCESS;
    FragmentEvent fragmentEvent = new FragmentEvent(fragment).setStatus(eventStatus);
    GraphNodeExecutionLog graphLog = GraphNodeExecutionLog.newInstance("id");

    FragmentExecutionLog executionLog = FragmentExecutionLog.newInstance(fragmentEvent, graphLog);

    // when
    FragmentExecutionLog result = new FragmentExecutionLog(executionLog.toJson());

    // then
    assertEquals(eventStatus, result.getStatus());
    assertEquals(fragment, result.getFragment());
    assertEquals(0, result.getStartTime());
    assertEquals(0, result.getFinishTime());
    assertEquals(graphLog, result.getGraph());
  }

}