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
package io.knotx.fragments.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FragmentOperationExceptionTest {

  public static final String ERROR_MESSAGE = "some error message";

  @Test
  @DisplayName("Expect all fields are deserialized")
  void expectAllFieldsDeserialized() {
    IllegalArgumentException error = new IllegalArgumentException(ERROR_MESSAGE);
    FragmentOperationError original = FragmentOperationError
        .newInstance(error);

    JsonObject serialized = original.toJson();
    FragmentOperationError copy = new FragmentOperationError(serialized);

    assertEquals(IllegalArgumentException.class.getCanonicalName(), copy.getClassName());
    assertEquals(ERROR_MESSAGE, copy.getMessage());
    List<String> stacktrace = copy.getStacktrace();
    assertFalse(stacktrace.isEmpty());
    assertTrue(
        stacktrace.get(0).contains("FragmentOperationExceptionTest.expectAllFieldsDeserialized"));
  }

}