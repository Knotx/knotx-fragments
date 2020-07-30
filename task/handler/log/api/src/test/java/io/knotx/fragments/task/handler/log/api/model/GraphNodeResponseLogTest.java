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

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GraphNodeResponseLogTest {

  public static final JsonObject SUCCESS_NODE_LOG = new JsonObject().put("debug", "true");
  public static final List<GraphNodeErrorLog> ERROR_LOG_LIST = Arrays.asList(
      GraphNodeErrorLog.newInstance(new IllegalArgumentException("some message")),
      GraphNodeErrorLog.newInstance(new IllegalArgumentException()));
  public static final JsonObject EMPTY_SUCCESS_NODE_LOG = new JsonObject();

  @Test
  @DisplayName("Expect success transition and node log are serialized correctly.")
  void expectSuccessNodeResponse() {
    // given
    GraphNodeResponseLog origin = GraphNodeResponseLog
        .newInstance(SUCCESS_TRANSITION, SUCCESS_NODE_LOG, Collections.emptyList());

    // when
    GraphNodeResponseLog tested = new GraphNodeResponseLog(origin.toJson());

    // then
    assertEquals(SUCCESS_TRANSITION, tested.getTransition());
    assertEquals(SUCCESS_NODE_LOG, tested.getLog());
    assertNotNull(tested.getErrors());
    assertTrue(tested.getErrors().isEmpty());
  }

  @Test
  @DisplayName("Expect custom transition and node log are serialized correctly.")
  void expectSuccessNodeResponseWithCustomTransition() {
    // given
    String expectedTransition = "custom";
    GraphNodeResponseLog origin = GraphNodeResponseLog
        .newInstance(expectedTransition, SUCCESS_NODE_LOG, Collections.emptyList());

    // when
    GraphNodeResponseLog tested = new GraphNodeResponseLog(origin.toJson());

    // then
    assertEquals(expectedTransition, tested.getTransition());
    assertEquals(SUCCESS_NODE_LOG, tested.getLog());
    assertNotNull(tested.getErrors());
    assertTrue(tested.getErrors().isEmpty());
  }

  @Test
  @DisplayName("Expect error transition and error logs are serialized correctly.")
  void expectErrorNodeResponse() {
    // given
    GraphNodeResponseLog origin = GraphNodeResponseLog
        .newInstance(ERROR_TRANSITION, EMPTY_SUCCESS_NODE_LOG, ERROR_LOG_LIST);

    // when
    GraphNodeResponseLog tested = new GraphNodeResponseLog(origin.toJson());

    // then
    assertEquals(ERROR_TRANSITION, tested.getTransition());
    assertEquals(EMPTY_SUCCESS_NODE_LOG, tested.getLog());
    assertEquals(ERROR_LOG_LIST, tested.getErrors());
  }

  @Test
  @DisplayName("Expect empty error log list when null passed.")
  void expectEmptyErrorsWhenNull() {
    // given
    GraphNodeResponseLog origin = GraphNodeResponseLog
        .newInstance(ERROR_TRANSITION, EMPTY_SUCCESS_NODE_LOG, null);

    // when
    GraphNodeResponseLog tested = new GraphNodeResponseLog(origin.toJson());

    // then
    assertNotNull(tested.getErrors());
    assertTrue(tested.getErrors().isEmpty());
  }

  @Test
  @DisplayName("Expect empty error log list when null passed.")
  void expectEmptyExecutionLogWhenNull() {
    // given
    GraphNodeResponseLog origin = GraphNodeResponseLog
        .newInstance(SUCCESS_TRANSITION, null, Collections.emptyList());

    // when
    GraphNodeResponseLog tested = new GraphNodeResponseLog(origin.toJson());

    // then
    assertNotNull(tested.getLog());
    assertEquals(EMPTY_SUCCESS_NODE_LOG, tested.getLog());
  }
}