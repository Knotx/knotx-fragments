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

import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class FragmentEventLogVerifier {

  private static final String ASSERTION_NOT_MATCH = "Log entries does not match!!\nExpected:\n%s,\ncurrent:\n%s";
  private static final String ASSERTION_DIFFERENT_SIZE = "Log entries does not have the same size! (expected: %s, current: %s)\nExpected:\n%s,\ncurrent:\n%s";


  static void verifyAllLogEntries(
      List<EventLogEntry> operations,
      Operation... expectedOperations) {

    if (operations.size() != expectedOperations.length) {
      throw new AssertionError(
          String.format(ASSERTION_DIFFERENT_SIZE, expectedOperations.length, operations.size(),
              Arrays.toString(expectedOperations), operations));
    }
    for (Operation expectedOperation : expectedOperations) {
      Position position = expectedOperation.getPosition();
      getSliceOfLog(operations, position)
          .filter(expectedOperation::matches)
          .findAny()
          .orElseThrow(() -> new AssertionError(
                  String.format(ASSERTION_NOT_MATCH, expectedOperation, operations)));
    }
  }

  static void verifyLogEntries(List<EventLogEntry> operations, Operation... expectedOperations) {
    for (Operation expectedOperation : expectedOperations) {
      Position position = expectedOperation.getPosition();
      getSliceOfLog(operations, position)
          .filter(expectedOperation::matches)
          .findAny()
          .orElseThrow(() -> new AssertionError(
              String.format(ASSERTION_NOT_MATCH, expectedOperation, operations)));
    }
  }

  private static Stream<EventLogEntry> getSliceOfLog(List<EventLogEntry> operations, Position position) {
    return operations.stream().skip(position.from())
        .limit(position.to() - position.from() + 1);
  }

  static final class Operation {

    private final String task;
    private final String node;
    private final String status;
    private final Position position;
    private final JsonObject nodeLog;
    private final Throwable error;

    private Operation(String task, String node, String status, Position position,
        JsonObject nodeLog, Throwable error) {
      this.task = task;
      this.node = node;
      this.status = status;
      this.position = position;
      this.nodeLog = nodeLog;
      this.error = error;
    }

    static Operation exact(String task, String node, String status, int position) {
      return new Operation(task, node, status, new ExactPosition(position), new JsonObject(), null);
    }

    static Operation exact(String task, String node, String status, int position, JsonObject nodeLog) {
      return new Operation(task, node, status, new ExactPosition(position), nodeLog, null);
    }

    static Operation exact(String task, String node, String status, int position, Throwable error) {
      return new Operation(task, node, status, new ExactPosition(position), new JsonObject(), error);
    }

    static Operation range(String task, String node, String status, int minPosition,
        int maxPosition) {
      return new Operation(task, node, status, new RangePosition(minPosition, maxPosition),
          new JsonObject(), null);
    }

    static Operation range(String task, String node, String status, int minPosition,
        int maxPosition, JsonObject nodeLog) {
      return new Operation(task, node, status, new RangePosition(minPosition, maxPosition),
          nodeLog, null);
    }

    static Operation range(String task, String node, String status, int minPosition,
        int maxPosition, Throwable error) {
      return new Operation(task, node, status, new RangePosition(minPosition, maxPosition),
          new JsonObject(), error);
    }

    public Position getPosition() {
      return position;
    }

    public boolean matches(EventLogEntry operation) {
      // to verify
      return task.equals(operation.getTask()) &&
          node.equals(operation.getNode()) &&
          status.equals(operation.getStatus().name()) &&
          Objects.equals(nodeLog, operation.getNodeLog()) &&
          Objects.equals(error, operation.getError());
    }

    @Override
    public String toString() {
      return "Operation{" +
          "task='" + task + '\'' +
          ", node='" + node + '\'' +
          ", status='" + status + '\'' +
          ", position=" + position +
          ", nodeLog=" + nodeLog +
          ", error=" + error +
          '}';
    }
  }

  interface Position {

    int from();

    int to();
  }

  static class ExactPosition implements Position {

    private final int currentPosition;

    ExactPosition(int currentPosition) {
      this.currentPosition = currentPosition;
    }

    @Override
    public int from() {
      return currentPosition;
    }

    @Override
    public int to() {
      return currentPosition;
    }

    @Override
    public String toString() {
      return "ExactPosition{" +
          "currentPosition=" + currentPosition +
          '}';
    }
  }

  static class RangePosition implements Position {

    private final int from;
    private final int to;

    RangePosition(int from, int to) {
      this.from = from;
      this.to = to;
    }

    @Override
    public int from() {
      return from;
    }

    @Override
    public int to() {
      return to;
    }

    @Override
    public String toString() {
      return "RangePosition{" +
          "from=" + from +
          ", to=" + to +
          '}';
    }
  }

}
