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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Iterator;

class FragmentEventLogVerifier {

  private static final String ASSERTION_MESSAGE = "Log entry is incorrect!\nExpected:\n%s,\ncurrent:\n%s";

  static void verifyLogEntries(JsonObject log, Operation... expectedOperations) {
    JsonArray logArray = log.getJsonArray("operations", new JsonArray());
    if (logArray.size() != expectedOperations.length) {
      throw new AssertionError(
          String.format(ASSERTION_MESSAGE, Arrays.toString(expectedOperations), logArray));
    }
    Iterator<JsonObject> operationsItr = logArray.stream()
        .map(operation -> (JsonObject) operation)
        .iterator();
    for (Operation expectedOperation : expectedOperations) {
      if (operationsItr.hasNext()) {
        JsonObject operation = operationsItr.next();
        boolean equal = expectedOperation.task.equals(operation.getString("task")) &&
            expectedOperation.name.equals(operation.getString("action")) &&
            expectedOperation.status.equals(operation.getString("status"));
        if (!equal) {
          throw new AssertionError(
              String.format(ASSERTION_MESSAGE, Arrays.toString(expectedOperations), logArray));
        }
      } else {
        throw new AssertionError(
            String.format(ASSERTION_MESSAGE, Arrays.toString(expectedOperations), logArray));
      }
    }
  }

  static final class Operation {

    private String task;
    private String name;
    private String status;

    private Operation(String task, String action, String status) {
      this.task = task;
      this.name = action;
      this.status = status;
    }

    static Operation of(String task, String action, String status) {
      return new Operation(task, action, status);
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

}
