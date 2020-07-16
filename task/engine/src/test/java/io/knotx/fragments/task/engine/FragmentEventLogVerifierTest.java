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
package io.knotx.fragments.task.engine;

import static org.junit.jupiter.api.Assertions.*;

import io.knotx.fragments.task.engine.FragmentEventLogVerifier.Operation;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FragmentEventLogVerifierTest {

  @Test
  @DisplayName("Expect verify all with no error when empty.")
  void expectVerifyAllNoErrorWhenEmpty() {
    List<EventLogEntry> operations = Collections.emptyList();
    FragmentEventLogVerifier.verifyAllLogEntries(operations);
  }

  @Test
  @DisplayName("Expect verify all with no error when the same.")
  void expectVerifyAllNoErrorWhenTheSame() {
    List<EventLogEntry> operations =
        Collections.singletonList(EventLogEntry.error("task", "a", "_error"));
    FragmentEventLogVerifier
        .verifyAllLogEntries(operations, Operation.exact("task", "a", "ERROR", 0));
  }

  @Test
  @DisplayName("Expect verify all with error when not same.")
  void expectVerifyAllErrorWhenNotSame() {
    List<EventLogEntry> operations =
        Collections.singletonList(EventLogEntry.error("task", "b", "_error", new JsonObject()));
    assertThrows(AssertionError.class, () -> FragmentEventLogVerifier
        .verifyAllLogEntries(operations, Operation.exact("task", "a", "ERROR", 0)));
  }

  @Test
  @DisplayName("Expect verify all with error when invalid position.")
  void expectVerifyAllErrorWhenInvalidPosition() {
    List<EventLogEntry> operations =
        Collections.singletonList(EventLogEntry.error("task", "b", "_error", new JsonObject()));
    assertThrows(AssertionError.class, () -> FragmentEventLogVerifier
        .verifyAllLogEntries(operations, Operation.exact("task", "a", "ERROR", 1)));
  }

  @Test
  @DisplayName("Expect verify all with error when elements count not same.")
  void expectVerifyAllErrorWhenSizeDiffers() {
    List<EventLogEntry> operations =
        Arrays.asList(
            EventLogEntry.error("task", "a", "_error", new JsonObject()),
            EventLogEntry.error("task", "b", "_error", new JsonObject()));
    assertThrows(AssertionError.class, () -> FragmentEventLogVerifier
        .verifyAllLogEntries(operations, Operation.exact("task", "a", "ERROR", 0)));
  }

  @Test
  @DisplayName("Expect verify with no error when empty.")
  void expectVerifyNoErrorWhenEmpty() {
    List<EventLogEntry> operations = Collections.emptyList();
    FragmentEventLogVerifier.verifyLogEntries(operations);
  }

  @Test
  @DisplayName("Expect verify with no error when the same.")
  void expectVerifyNoErrorWhenTheSame() {
    List<EventLogEntry> operations =
        Collections.singletonList(EventLogEntry.error("task", "a", "_error", new JsonObject()));
    FragmentEventLogVerifier
        .verifyLogEntries(operations, Operation.exact("task", "a", "ERROR", 0));
  }

  @Test
  @DisplayName("Expect verify with error when not same.")
  void expectVerifyErrorWhenNotSame() {
    List<EventLogEntry> operations =
        Collections.singletonList(EventLogEntry.error("task", "b", "_error", new JsonObject()));
    assertThrows(AssertionError.class, () -> FragmentEventLogVerifier
        .verifyLogEntries(operations, Operation.exact("task", "a", "ERROR", 0)));
  }

  @Test
  @DisplayName("Expect verify with error when invalid position.")
  void expectVerifyErrorWhenInvalidPosition() {
    List<EventLogEntry> operations =
        Collections.singletonList(EventLogEntry.error("task", "b", "_error", new JsonObject()));
    assertThrows(AssertionError.class, () -> FragmentEventLogVerifier
        .verifyLogEntries(operations, Operation.exact("task", "a", "ERROR", 1)));
  }

  @Test
  @DisplayName("Expect verify when elements count not same.")
  void expectVerifyNoErrorWhenSizeDiffers() {
    List<EventLogEntry> operations =
        Arrays.asList(
            EventLogEntry.error("task", "a", "_error", new JsonObject()),
            EventLogEntry.error("task", "b", "_error", new JsonObject()));
    FragmentEventLogVerifier
        .verifyLogEntries(operations, Operation.exact("task", "a", "ERROR", 0));
  }

  @Test
  @DisplayName("Expect verify when elements in range.")
  void expectVerifyNoErrorWhenInRange() {
    List<EventLogEntry> operations =
        Arrays.asList(
            EventLogEntry.error("task", "a", "_error", new JsonObject()),
            EventLogEntry.error("task", "b", "_error", new JsonObject()));
    FragmentEventLogVerifier
        .verifyLogEntries(operations, Operation.range("task", "a", "ERROR", 0, 1));
  }

  @Test
  @DisplayName("Expect verify when elements in range.")
  void expectVerifyErrorWhenNotInRange() {
    List<EventLogEntry> operations =
        Arrays.asList(
            EventLogEntry.error("task", "a", "_error", new JsonObject()),
            EventLogEntry.error("task", "b", "_error", new JsonObject()));
    assertThrows(AssertionError.class, () -> FragmentEventLogVerifier
        .verifyLogEntries(operations, Operation.range("task", "a", "ERROR", 1, 2)));
  }
}