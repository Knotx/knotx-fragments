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
package io.knotx.fragments.handler.consumer.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.EventLogEntry;
import io.knotx.fragments.engine.EventLogEntry.NodeStatus;
import io.knotx.fragments.engine.api.node.single.FragmentResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventLogConverterTest {

  private static final String TASK_NAME = "test-event-log";
  private static final String NODE_ID = "1234-4321-1234";

  private EventLogConverter tested;

  @Test
  @DisplayName("Expect _logStatus=missing and status=MISSING when log does not contain any entries")
  void fillWithEmptyLog() {
    givenEmptyLogConverter();

    JsonObject output = tested.fillWithLog(new JsonObject(), NODE_ID);

    JsonObject expected = new JsonObject().put("_logStatus", "MISSING")
        .put("status", NodeStatus.MISSING);

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect _logStatus=missing and status=MISSING when log does not contain entries for the given node")
  void fillWithMissingLogEntries() {
    givenLogConverter(
        EventLogEntry.success(TASK_NAME, "non-existent", successFragmentResult())
    );

    JsonObject output = tested.fillWithLog(new JsonObject(), NODE_ID);

    JsonObject expected = new JsonObject()
        .put("status", NodeStatus.MISSING)
        .put("_logStatus", "MISSING");

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect _logStatus=ok when single success log entry for node")
  void fillWithSingleSuccessLogEntry() {
    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.success(TASK_NAME, NODE_ID, successFragmentResult(nodeLog())),
        EventLogEntry.success(TASK_NAME, "non-existent", successFragmentResult()),
        EventLogEntry.error(TASK_NAME, "non-existent", "timeout"),
    };

    givenLogConverter(logs);

    JsonObject output = tested.fillWithLog(new JsonObject(), NODE_ID);

    JsonObject expected = new JsonObject().put("_logStatus", "ok")
        .put("status", NodeStatus.SUCCESS)
        .put("response", new JsonObject()
            .put("transition", "_success")
            .put("invocations", new JsonArray().add(nodeLog())));

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect _logStatus=ok when single error log entry for node")
  void fillWithSingleErrorLogEntry() {
    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.error(TASK_NAME, NODE_ID, "_error"),
        EventLogEntry.success(TASK_NAME, "non-existent", successFragmentResult()),
        EventLogEntry.error(TASK_NAME, "non-existent", "timeout"),
    };

    givenLogConverter(logs);

    JsonObject output = tested.fillWithLog(new JsonObject(), NODE_ID);

    JsonObject expected = new JsonObject().put("_logStatus", "ok")
        .put("status", NodeStatus.ERROR)
        .put("response", new JsonObject()
            .put("transition", "_error")
            .put("invocations", new JsonArray()));

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect _logStatus=ok when double log entry for node with unsupported transition")
  void fillWithDoubleLogEntry() {
    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.success(TASK_NAME, NODE_ID, successFragmentResult(nodeLog())),
        EventLogEntry.unsupported(TASK_NAME, NODE_ID, "_success"),
        EventLogEntry.success(TASK_NAME, "non-existent", successFragmentResult()),
        EventLogEntry.error(TASK_NAME, "non-existent", "timeout"),
    };

    givenLogConverter(logs);

    JsonObject output = tested.fillWithLog(new JsonObject(), NODE_ID);

    JsonObject expected = new JsonObject().put("_logStatus", "ok")
        .put("status", NodeStatus.UNSUPPORTED_TRANSITION)
        .put("response", new JsonObject()
            .put("transition", "_success")
            .put("invocations", new JsonArray().add(nodeLog())));

    assertEquals(expected, output);
  }

  @Test
  @DisplayName("Expect _logStatus=multiple when given multiple log entries for node")
  void fillWithMultipleLogEntries() {
    EventLogEntry[] logs = new EventLogEntry[]{
        EventLogEntry.error(TASK_NAME, NODE_ID, "_error"),
        EventLogEntry.unsupported(TASK_NAME, NODE_ID, "_error"),
        EventLogEntry.success(TASK_NAME, NODE_ID, successFragmentResult())
    };

    givenLogConverter(logs);

    JsonObject output = tested.fillWithLog(new JsonObject(), NODE_ID);

    JsonObject expected = new JsonObject().put("_logStatus", "multiple")
        .put("_rawLogs",
            fromList(Arrays.stream(logs).filter(logEntry -> logEntry.getNode().equals(NODE_ID))
                .map(EventLogEntry::toJson).collect(Collectors.toList())));

    assertEquals(expected, output);
  }

  void givenEmptyLogConverter() {
    tested = new EventLogConverter(Collections.emptyList());
  }

  void givenLogConverter(EventLogEntry... entries) {
    tested = new EventLogConverter(Arrays.asList(entries));
  }

  private FragmentResult successFragmentResult() {
    return successFragmentResult(null);
  }

  private FragmentResult successFragmentResult(JsonObject nodeLog) {
    return new FragmentResult(
        new Fragment("dummy", new JsonObject(), ""),
        "_success",
        nodeLog
    );
  }

  private <T> JsonArray fromList(List<T> list) {
    JsonArray output = new JsonArray();
    list.forEach(output::add);
    return output;
  }

  private JsonObject nodeLog() {
    return new JsonObject()
        .put("alias", "alias")
        .put("logs", new JsonObject())
        .put("doActionLogs", new JsonArray());
  }

}
