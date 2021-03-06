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
package io.knotx.fragments.task.handler.consumer;

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.engine.EventLogEntry;
import io.knotx.fragments.task.handler.log.api.model.LoggedNodeStatus;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventLogConverterTest {

  private static final String TASK_NAME = "test-event-log";
  private static final String NODE_ID = "1234-4321-1234";
  private static final String OTHER_NODE_ID = "other-node-id";

  @Test
  @DisplayName("Expect status=UNPROCESSED when log does not contain any entries")
  void fillWithEmptyLog() {
    EventLogConverter tested = givenEmptyLogConverter();

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.UNPROCESSED, result.getStatus());
  }

  @Test
  @DisplayName("Expect status=UNPROCESSED when log does not contain entries for the given node")
  void fillWithMissingLogEntries() {
    EventLogConverter tested = givenLogConverter(
        EventLogEntry.success(TASK_NAME, OTHER_NODE_ID, successFragmentResult())
    );

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.UNPROCESSED, result.getStatus());
  }

  @Test
  @DisplayName("Expect status=SUCCESS when single success log entry for node")
  void fillWithSingleSuccessLogEntry() {
    JsonObject nodeLog = nodeLog();
    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.success(TASK_NAME, NODE_ID, successFragmentResult(nodeLog)),
        EventLogEntry.success(TASK_NAME, OTHER_NODE_ID, successFragmentResult())
    };
    EventLogConverter tested = givenLogConverter(logs);

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.SUCCESS, result.getStatus());
    assertNotNull(result.getResponse());
    assertEquals(SUCCESS_TRANSITION, result.getResponse().getTransition());
    assertEquals(nodeLog, result.getResponse().getLog());
  }

  @Test
  @DisplayName("Expect status=ERROR when single error log entry for node")
  void fillWithSingleErrorLogEntry() {
    JsonObject nodeLog = nodeLog();
    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.error(TASK_NAME, NODE_ID, ERROR_TRANSITION, nodeLog),
        EventLogEntry.success(TASK_NAME, OTHER_NODE_ID, successFragmentResult())
    };
    EventLogConverter tested = givenLogConverter(logs);

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.ERROR, result.getStatus());
    assertNotNull(result.getResponse());
    assertEquals(ERROR_TRANSITION, result.getResponse().getTransition());
    assertEquals(nodeLog, result.getResponse().getLog());
  }

  @Test
  @DisplayName("Expect status=ERROR when error and unsupported log entries for node")
  void fillWithErrorAndUnsupportedLogEntries() {
    JsonObject nodeLog = nodeLog();
    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.error(TASK_NAME, NODE_ID, ERROR_TRANSITION, nodeLog),
        EventLogEntry.unsupported(TASK_NAME, NODE_ID, ERROR_TRANSITION),
        EventLogEntry.success(TASK_NAME, OTHER_NODE_ID, successFragmentResult())
    };
    EventLogConverter tested = givenLogConverter(logs);

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.ERROR, result.getStatus());
    assertNotNull(result.getResponse());
    assertEquals(ERROR_TRANSITION, result.getResponse().getTransition());
    assertEquals(nodeLog, result.getResponse().getLog());
  }

  @Test
  @DisplayName("Expect status=ERROR when single exception log entry for node")
  void fillWithSingleExceptionLogEntry() {
    Throwable error = new IllegalArgumentException("error message");

    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.exception(TASK_NAME, NODE_ID, ERROR_TRANSITION, error),
        EventLogEntry.success(TASK_NAME, OTHER_NODE_ID, successFragmentResult())
    };
    EventLogConverter tested = givenLogConverter(logs);

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.ERROR, result.getStatus());
    assertNotNull(result.getResponse());
    assertEquals(ERROR_TRANSITION, result.getResponse().getTransition());
    assertEquals(Collections.singletonList(error), result.getResponse().getErrors());
  }

  @Test
  @DisplayName("Expect status=ERROR when exception and unsupported log entries for node")
  void fillWithExceptionAndUnsupportedLogEntries() {
    Throwable error = new IllegalArgumentException("error message");

    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.exception(TASK_NAME, NODE_ID, ERROR_TRANSITION, error),
        EventLogEntry.unsupported(TASK_NAME, NODE_ID, ERROR_TRANSITION),
        EventLogEntry.success(TASK_NAME, OTHER_NODE_ID, successFragmentResult())
    };
    EventLogConverter tested = givenLogConverter(logs);

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.ERROR, result.getStatus());
    assertNotNull(result.getResponse());
    assertEquals(ERROR_TRANSITION, result.getResponse().getTransition());
    assertEquals(Collections.singletonList(error), result.getResponse().getErrors());
  }


  @Test
  @DisplayName("Expect status=ERROR when single error log entry with throwable for node")
  void fillWithSingleCompositeExceptionLogEntry() {
    CompositeException composite = new CompositeException(
        new IllegalArgumentException("error message 1"),
        new IllegalArgumentException("error message 2")
    );

    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.exception(TASK_NAME, NODE_ID, ERROR_TRANSITION, composite),
        EventLogEntry.success(TASK_NAME, OTHER_NODE_ID, successFragmentResult())
    };
    EventLogConverter tested = givenLogConverter(logs);

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.ERROR, result.getStatus());
    assertNotNull(result.getResponse());
    assertEquals(ERROR_TRANSITION, result.getResponse().getTransition());
    assertEquals(composite.getExceptions(), result.getResponse().getErrors());
  }

  @Test
  @DisplayName("Expect status=OTHER when there's a single success log entry with custom transition")
  void fillWithSingleSuccessCustomLogEntry() {
    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.success(TASK_NAME, NODE_ID, successFragmentResult(nodeLog(), true)),
        EventLogEntry.success(TASK_NAME, OTHER_NODE_ID, successFragmentResult())
    };
    EventLogConverter tested = givenLogConverter(logs);

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.OTHER, result.getStatus());
    assertNotNull(result.getResponse());
    assertEquals("custom", result.getResponse().getTransition());
    assertEquals(nodeLog(), result.getResponse().getLog());
  }

  @Test
  @DisplayName("Expect status=SUCCESS when there's an unsupported success transition")
  void fillWithDoubleLogSuccessEntry() {
    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.success(TASK_NAME, NODE_ID, successFragmentResult(nodeLog())),
        EventLogEntry.unsupported(TASK_NAME, NODE_ID, SUCCESS_TRANSITION),
        EventLogEntry.success(TASK_NAME, OTHER_NODE_ID, successFragmentResult()),
        EventLogEntry.error(TASK_NAME, OTHER_NODE_ID, "timeout")
    };
    EventLogConverter tested = givenLogConverter(logs);

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.SUCCESS, result.getStatus());
    assertNotNull(result.getResponse());
    assertEquals(SUCCESS_TRANSITION, result.getResponse().getTransition());
    assertEquals(nodeLog(), result.getResponse().getLog());
  }

  @Test
  @DisplayName("Expect unsupported non-success transition to have no effect")
  void fillWithDoubleLogNonSuccessEntry() {
    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.error(TASK_NAME, NODE_ID, ERROR_TRANSITION),
        EventLogEntry.unsupported(TASK_NAME, NODE_ID, ERROR_TRANSITION),
        EventLogEntry.success(TASK_NAME, OTHER_NODE_ID, successFragmentResult()),
        EventLogEntry.error(TASK_NAME, OTHER_NODE_ID, "timeout")
    };
    EventLogConverter tested = givenLogConverter(logs);

    NodeExecutionData result = tested.getExecutionData(NODE_ID);

    assertEquals(LoggedNodeStatus.ERROR, result.getStatus());
    assertNotNull(result.getResponse());
    assertEquals(ERROR_TRANSITION, result.getResponse().getTransition());
    assertEquals(new JsonObject(), result.getResponse().getLog());
  }

  EventLogConverter givenEmptyLogConverter() {
    return new EventLogConverter(Collections.emptyList());
  }

  EventLogConverter givenLogConverter(EventLogEntry... entries) {
    return new EventLogConverter(Arrays.asList(entries));
  }

  private FragmentResult successFragmentResult() {
    return successFragmentResult(null);
  }

  private FragmentResult successFragmentResult(JsonObject nodeLog) {
    return successFragmentResult(nodeLog, false);
  }

  private FragmentResult successFragmentResult(JsonObject nodeLog, boolean customTransition) {
    return new FragmentResult(
        new Fragment("dummy", new JsonObject(), ""),
        customTransition ? "custom" : SUCCESS_TRANSITION,
        nodeLog
    );
  }

  private JsonObject nodeLog() {
    return new JsonObject()
        .put("alias", "alias")
        .put("logs", new JsonObject())
        .put("invocations", new JsonArray());
  }

}
