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
package io.knotx.fragments.action.api.invoker;

import static io.knotx.fragments.action.api.invoker.TestUtils.someContext;
import static io.knotx.fragments.action.api.invoker.TestUtils.successResult;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActionInvocationTest {

  @Test
  @DisplayName("Expect rethrown wrapped checked exception when invocation failed")
  void rethrowCheckedException() {
    IOException exception = new IOException();
    ActionInvocation tested = ActionInvocation.exception(10, exception, someContext());
    Throwable thrown = assertThrows(RuntimeException.class, tested::rethrowIfResultNotDelivered);
    assertEquals(exception, thrown.getCause());
  }

  @Test
  @DisplayName("Expect rethrown unchecked exception when invocation failed")
  void rethrowException() {
    IllegalStateException exception = new IllegalStateException();
    ActionInvocation tested = ActionInvocation.exception(10, exception, someContext());
    Throwable thrown = assertThrows(IllegalStateException.class, tested::rethrowIfResultNotDelivered);
    assertEquals(exception, thrown);
  }

  @Test
  @DisplayName("Expect nothing rethrown when invocation succeeded")
  void dontRethrowDelivered() {
    ActionInvocation tested = ActionInvocation.resultDelivered(10, successResult());
    assertDoesNotThrow(tested::rethrowIfResultNotDelivered);
  }

  @Test
  @DisplayName("Expect rethrown wrapped TimeoutException when invocation timed out")
  void rethrowTimeout() {
    ActionInvocation tested = ActionInvocation.timeout(10, someContext());
    Throwable thrown = assertThrows(RuntimeException.class, tested::rethrowIfResultNotDelivered);
    assertTrue(thrown.getCause() instanceof TimeoutException);
  }

}
