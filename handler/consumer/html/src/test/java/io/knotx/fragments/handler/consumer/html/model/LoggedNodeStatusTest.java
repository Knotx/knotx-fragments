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
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.engine.api.EventLogEntry;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class LoggedNodeStatusTest {

  private static final String TASK_NAME = "some-task";
  private static final String ROOT_NODE = "1234-4321-1234";

  @Test
  @DisplayName("Entry with success transition matches SUCCESS status")
  void entryWithSuccessTransitionMatchesSuccessStatus() {
    // given
    EventLogEntry entry = EventLogEntry.success(TASK_NAME, ROOT_NODE, createFragmentResult("_success"));

    // when
    LoggedNodeStatus status = LoggedNodeStatus.from(entry);

    // then
    assertEquals(LoggedNodeStatus.SUCCESS, status);
  }

  @Test
  @DisplayName("Entry with error transition matches ERROR status")
  void entryWithErrorTransitionMatchesSuccessStatus() {
    // given
    EventLogEntry entry = EventLogEntry.error(TASK_NAME, ROOT_NODE, "_error");

    // when
    LoggedNodeStatus status = LoggedNodeStatus.from(entry);

    // then
    assertEquals(LoggedNodeStatus.ERROR, status);
  }

  @Test
  @DisplayName("Timed-out entry matches ERROR status")
  void timedOutEntryMatchesErrorStatus() {
    // given
    EventLogEntry entry = EventLogEntry.timeout(TASK_NAME, ROOT_NODE);

    // when
    LoggedNodeStatus status = LoggedNodeStatus.from(entry);

    // then
    assertEquals(LoggedNodeStatus.ERROR, status);
  }

  @Test
  @DisplayName("Entry with custom transition matches OTHER status")
  void entryWithCustomTransitionMatchesOtherStatus() {
    // given
    EventLogEntry entry = EventLogEntry.success(TASK_NAME, ROOT_NODE, createFragmentResult("custom"));

    // when
    LoggedNodeStatus status = LoggedNodeStatus.from(entry);

    // then
    assertEquals(LoggedNodeStatus.OTHER, status);
  }

  @Test
  @DisplayName("Unprocessed entry matches UNPROCESSED status")
  void unprocessedEntryMatchesUnprocessedStatus() {
    // given
    EventLogEntry entry = EventLogEntry.unprocessed(TASK_NAME, ROOT_NODE);

    // when
    LoggedNodeStatus status = LoggedNodeStatus.from(entry);

    // then
    assertEquals(LoggedNodeStatus.UNPROCESSED, status);
  }

  @Test
  @DisplayName("Entry with unsupported transition status has no match and throws exception")
  void entryWithUnsupportedTransitionHasNoMatchAndThrowsException() {
    // given
    EventLogEntry entry = EventLogEntry.unsupported(TASK_NAME, ROOT_NODE, "unknown");

    // when
    Executable matching = () -> LoggedNodeStatus.from(entry);

    // then
    assertThrows(IllegalArgumentException.class, matching);
  }

  private FragmentResult createFragmentResult(String transition) {
    return new FragmentResult(new Fragment("_empty", new JsonObject(), ""), transition, new JsonObject());
  }
}