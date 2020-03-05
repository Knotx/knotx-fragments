package io.knotx.fragments.handler;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.EventLogEntry;
import io.knotx.fragments.engine.api.node.single.FragmentResult;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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