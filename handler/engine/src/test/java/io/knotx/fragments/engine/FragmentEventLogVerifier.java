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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.fragments.engine;

import java.util.Arrays;
import java.util.stream.Stream;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class FragmentEventLogVerifier {

  private static final String ASSERTION_NOT_MATCH = "Log entries does not match!!\nExpected:\n%s,\ncurrent:\n%s";
  private static final String ASSERTION_DIFFERENT_SIZE = "Log entries does not have the same size!\nExpected:\n%s,\ncurrent:\n%s";


  static void verifyAllLogEntries(JsonObject log, Operation... expectedOperations) {
    JsonArray logArray = log.getJsonArray("operations", new JsonArray());
    if (logArray.size() != expectedOperations.length) {
      throw new AssertionError(
          String.format(ASSERTION_DIFFERENT_SIZE, Arrays.toString(expectedOperations), logArray));
    }
    for (Operation expectedOperation : expectedOperations) {
      Position position = expectedOperation.getPosition();
      getSliceOfLog(logArray, position)
          .filter(expectedOperation::matches)
          .findAny()
          .orElseThrow(() -> new AssertionError(
                  String.format(ASSERTION_NOT_MATCH, Arrays.toString(expectedOperations), logArray)));
    }
  }

  static void verifyLogEntries(JsonObject log, Operation... expectedOperations) {
    JsonArray logArray = log.getJsonArray("operations", new JsonArray());
    for (Operation expectedOperation : expectedOperations) {
      Position position = expectedOperation.getPosition();
      getSliceOfLog(logArray, position)
          .filter(expectedOperation::matches)
          .findAny()
          .orElseThrow(() -> new AssertionError(
              String.format(ASSERTION_NOT_MATCH, Arrays.toString(expectedOperations), logArray)));
    }
  }

  private static Stream<JsonObject> getSliceOfLog(JsonArray logArray, Position position) {
    return logArray.stream().skip(position.from())
        .limit(position.to() - position.from() + 1)
        .map(JsonObject.class::cast);
  }

  static final class Operation {

    private final String task;
    private final String name;
    private final String status;
    private final Position position;

    private Operation(String task, String action, String status, Position position) {
      this.task = task;
      this.name = action;
      this.status = status;
      this.position = position;
    }

    static Operation exact(String task, String action, String status, int position) {
      return new Operation(task, action, status, new ExactPosition(position));
    }

    static Operation range(String task, String action, String status, int minPosition,
        int maxPosition) {
      return new Operation(task, action, status, new RangePosition(minPosition, maxPosition));
    }

    public Position getPosition() {
      return position;
    }

    public boolean matches(JsonObject operation) {
      return task.equals(operation.getString("task")) &&
          name.equals(operation.getString("action")) &&
          status.equals(operation.getString("status"));
    }

    @Override
    public String toString() {
      return "Operation{" +
          "task='" + task + '\'' +
          ", name='" + name + '\'' +
          ", status='" + status + '\'' +
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
  }

}
