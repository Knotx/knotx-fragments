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

import static io.knotx.fragments.action.library.TestUtils.someContext;
import static io.knotx.fragments.action.library.TestUtils.successResult;
import static io.knotx.fragments.action.library.cb.CircuitBreakerAction.ERROR_LOG_KEY;
import static io.knotx.fragments.action.library.cb.CircuitBreakerAction.INVOCATION_COUNT_LOG_KEY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.knotx.fragments.action.api.invoker.ActionInvocation;
import io.knotx.fragments.action.api.log.ActionLogger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerActionLoggerTest {

  @Mock
  private ActionLogger actionLogger;

  private CircuitBreakerActionLogger tested;

  @BeforeEach
  void setUp() {
    tested = new CircuitBreakerActionLogger(actionLogger);
  }

  @Test
  @DisplayName("Expect succeeded invocation logged on info")
  void succeededInvocationOnInfo() {
    ActionInvocation invocation = succeededInvocation();
    tested.onInvocation();
    tested.onSuccess(invocation);

    verify(actionLogger, times(1)).info(invocation);
  }

  @Test
  @DisplayName("Expect failed invocation logged on error")
  void failedInvocationOnError() {
    ActionInvocation invocation = failedInvocation();
    tested.onInvocation();
    tested.onFailure(invocation);

    verify(actionLogger, times(1)).error(invocation);
  }

  @Test
  @DisplayName("Expect fallback logged on error")
  void fallbackOnError() {
    tested.onInvocation();
    tested.onFallback(new RuntimeException());

    verify(actionLogger, times(1)).error(eq(ERROR_LOG_KEY), anyString());
  }

  @Test
  @DisplayName("Expect counted invocations on info when 5 failures and 1 success")
  void countInvocationsAndSucceed() {
    IntStream.rangeClosed(1, 5).forEach(i -> {
      tested.onInvocation();
      tested.onFailure(failedInvocation());
    });

    tested.onInvocation();
    tested.onSuccess(succeededInvocation());

    verify(actionLogger, times(1)).info(INVOCATION_COUNT_LOG_KEY, "6");
  }

  @Test
  @DisplayName("Expect counted invocations on error when 5 failures and 1 fallback")
  void countInvocationsAndFallback() {
    IntStream.rangeClosed(1, 5).forEach(i -> {
      tested.onInvocation();
      tested.onFailure(failedInvocation());
    });

    tested.onInvocation();
    tested.onFallback(new RuntimeException());

    verify(actionLogger, times(1)).error(INVOCATION_COUNT_LOG_KEY, "6");
  }

  private ActionInvocation succeededInvocation() {
    return ActionInvocation.resultDelivered(100, successResult());
  }

  private ActionInvocation failedInvocation() {
    return ActionInvocation.exception(100, new RuntimeException(), someContext());
  }

}
