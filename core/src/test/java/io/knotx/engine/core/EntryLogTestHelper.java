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
package io.knotx.engine.core;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Iterator;
import java.util.List;

class EntryLogTestHelper {

  private static final String ASSERTION_MESSAGE = "They are equal, current [%s], expected [%s]";

  static boolean verifyLogEntries(JsonObject log, List<Operation> expectedList) {
    JsonArray logArray = log.getJsonArray("operations", new JsonArray());
    if (logArray.size() != expectedList.size()) {
      return false;
    }
    Iterator<JsonObject> operationsItr = logArray.stream()
        .map(operation -> (JsonObject) operation)
        .iterator();
    for (Operation expected : expectedList) {
      if (operationsItr.hasNext()) {
        JsonObject operation = operationsItr.next();
        boolean equal = expected.consumer.equals(operation.getString("consumer")) &&
            expected.action.equals(operation.getString("action"));
        if (!equal) {
          throw new AssertionError(String.format(ASSERTION_MESSAGE, logArray, expectedList));
        }
      } else {
        throw new AssertionError(String.format(ASSERTION_MESSAGE, logArray, expectedList));
      }
    }
    return true;
  }

  static final class Operation {

    private String consumer;
    private String action;

    private Operation(String consumer, String action) {
      this.consumer = consumer;
      this.action = action;
    }

    static Operation of(String consumer, String action) {
      return new Operation(consumer, action);
    }

    @Override
    public String toString() {
      return "{" +
          "consumer='" + consumer + '\'' +
          ", action='" + action + '\'' +
          '}';
    }
  }


}
