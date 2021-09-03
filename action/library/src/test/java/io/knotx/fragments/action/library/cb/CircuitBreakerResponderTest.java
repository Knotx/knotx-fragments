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
package io.knotx.fragments.action.library.cb;

import static io.knotx.fragments.action.library.TestUtils.failedResult;
import static io.knotx.fragments.action.library.TestUtils.someContext;
import static io.knotx.fragments.action.library.TestUtils.successResult;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableSet;
import io.knotx.fragments.action.api.invoker.ActionInvocation;
import io.knotx.fragments.action.library.exception.DoActionExecuteException;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerResponderTest {

  private static final Set<String> ERROR_TRANSITIONS = ImmutableSet
      .of("_error", "_exception", "_external_timeout");

  private CircuitBreakerResponder tested;
  private Promise<FragmentResult> promise;

  @Mock
  private CircuitBreakerActionLogger logger;

  @BeforeEach
  void setUp() {
    tested = new CircuitBreakerResponder(ERROR_TRANSITIONS);
    promise = Promise.promise();
    lenient().when(logger.logAsJson()).thenReturn(new JsonObject());
  }

  @Test
  @DisplayName("Expect completion when delivered FragmentResult with accepted transition")
  void completedWhenTransitionAccepted() {
    FragmentResult result = successResult();
    tested.respond(promise, delivered(result), logger);

    assertEquals(result.getFragment(), promise.future().result().getFragment());
    assertEquals(result.getTransition(), promise.future().result().getTransition());
    assertEquals(result.getError(), promise.future().result().getError());
  }

  @Test
  @DisplayName("Expect success invocation logged when delivered FragmentResult with accepted transition")
  void loggedWhenTransitionAccepted() {
    ActionInvocation invocation = delivered(successResult());
    tested.respond(promise, invocation, logger);

    verify(logger, times(1)).onSuccess(invocation);
  }

  @Test
  @DisplayName("Expect failing when delivered FragmentResult with erroneous transition")
  void failedWhenTransitionErroneous() {
    tested.respond(promise, delivered(failedResult()), logger);

    assertTrue(promise.future().cause() instanceof DoActionExecuteException);
  }

  @Test
  @DisplayName("Expect failed invocation logged when delivered FragmentResult with accepted transition")
  void loggedWhenTransitionErroneous() {
    ActionInvocation invocation = delivered(failedResult());
    tested.respond(promise, invocation, logger);

    verify(logger, times(1)).onFailure(invocation);
  }

  @Test
  @DisplayName("Expect failing when FragmentResult not delivered")
  void failedWhenResultNotDelivered() {
    Throwable cause = new RuntimeException();
    tested.respond(promise, notDelivered(cause), logger);

    assertEquals(cause, promise.future().cause());
  }

  @Test
  @DisplayName("Expect failed invocation logged when FragmentResult not delivered")
  void loggedWhenResultNotDelivered() {
    ActionInvocation invocation = notDelivered(new RuntimeException());
    tested.respond(promise, invocation, logger);

    verify(logger, times(1)).onFailure(invocation);
  }

  private ActionInvocation delivered(FragmentResult result) {
    return ActionInvocation.resultDelivered(100, result);
  }

  private ActionInvocation notDelivered(Throwable cause) {
    return ActionInvocation.exception(100, cause, someContext());
  }

}
