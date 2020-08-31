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

import static io.knotx.fragments.api.FragmentOperationFailure.GENERAL_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.reactivex.exceptions.CompositeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FragmentOperationFailureTest {

  @Test
  @DisplayName("Expect valid deserialized error code and message.")
  void expectErrorCodeAndMessage() {
    String errorCode = "ERROR_CODE";
    String errorMessage = "some error message";
    FragmentOperationFailure origin = FragmentOperationFailure
        .newInstance(errorCode, errorMessage);

    FragmentOperationFailure copy = new FragmentOperationFailure(origin.toJson());

    assertEquals(origin, copy);
    assertEquals(errorCode, copy.getCode());
    assertEquals(errorMessage, copy.getMessage());
    assertNotNull(copy.getExceptions());
    assertTrue(copy.getExceptions().isEmpty());
  }

  @Test
  @DisplayName("Expect valid deserialized exception.")
  void expectExceptionDeserialized() {
    String errorMessage = "some error message";
    FragmentOperationFailure origin = FragmentOperationFailure
        .newInstance(new IllegalArgumentException(errorMessage));

    FragmentOperationFailure copy = new FragmentOperationFailure(origin.toJson());

    assertEquals(origin, copy);
    assertEquals(GENERAL_EXCEPTION, copy.getCode());
    assertEquals(errorMessage, copy.getMessage());
    assertNotNull(copy.getExceptions());
    assertEquals(1, copy.getExceptions().size());
  }

  @Test
  @DisplayName("Expect null error message when exception has no detail message.")
  void expectEmptyErrorMessage() {
    FragmentOperationFailure origin = FragmentOperationFailure
        .newInstance(new IllegalArgumentException());

    FragmentOperationFailure copy = new FragmentOperationFailure(origin.toJson());
    assertEquals(origin, copy);
    assertNull(copy.getMessage());
  }

  @Test
  @DisplayName("Expect valid deserialized composite exception.")
  void expectCompositeExceptionDeserialized() {
    IllegalArgumentException firstException = new IllegalArgumentException();
    IllegalStateException secondException = new IllegalStateException();
    CompositeException error = new CompositeException(firstException, secondException);
    FragmentOperationFailure origin = FragmentOperationFailure
        .newInstance(error);

    FragmentOperationFailure copy = new FragmentOperationFailure(origin.toJson());

    assertEquals(origin, copy);
    assertEquals(GENERAL_EXCEPTION, copy.getCode());
    assertEquals("2 exceptions occurred. ", copy.getMessage());
    assertNotNull(copy.getExceptions());
    assertEquals(2, copy.getExceptions().size());
    assertEquals(firstException.getClass().getCanonicalName(),
        copy.getExceptions().get(0).getClassName());
    assertEquals(secondException.getClass().getCanonicalName(),
        copy.getExceptions().get(1).getClassName());
  }

}