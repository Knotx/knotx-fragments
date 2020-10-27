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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.FutureAction;
import io.knotx.fragments.action.api.invoker.ActionInvocation.Status;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.Future;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith(VertxExtension.class)
@Timeout(value = 5, timeUnit = SECONDS)
class ActionInvokerTest {

  @Test
  @DisplayName("Expect ActionInvocation with RESULT_DELIVERED status when action called handler with succeeded AsyncResult with delivered Fragment")
  void succeededEmptyAsyncResult(VertxTestContext testContext) {
    FragmentResult result = successResult();
    Action action = (FutureAction) context -> Future.succeededFuture(result);

    verifyCompletion(testContext, action, invocation -> {
      assertEquals(Status.RESULT_DELIVERED, invocation.getStatus());
      assertEquals(result, invocation.getFragmentResult());
    });
  }

  @Test
  @DisplayName("Expect ActionInvocation with EXCEPTION status when action called handler with succeeded empty AsyncResult")
  void succeededFatAsyncResult(VertxTestContext testContext) {
    Action action = (FutureAction) context -> Future.succeededFuture();

    verifyCompletion(testContext, action, invocation -> {
      assertEquals(Status.EXCEPTION, invocation.getStatus());
      assertEquals(IllegalStateException.class, invocation.getError().getClass());
    });
  }

  @Test
  @DisplayName("Expect ActionInvocation with EXCEPTION status when action called handler with failed Future")
  void failedFuture(VertxTestContext testContext) {
    RuntimeException exception = new RuntimeException();
    Action action = (FutureAction) context -> Future.failedFuture(exception);

    verifyCompletion(testContext, action, invocation -> {
      assertEquals(Status.EXCEPTION, invocation.getStatus());
      assertEquals(exception, invocation.getError());
    });
  }

  @Test
  @DisplayName("Expect ActionInvocation with EXCEPTION status when action throws")
  void actionThrows(VertxTestContext testContext) {
    RuntimeException exception = new RuntimeException();
    Action action = (context, handler) -> {
      throw exception;
    };

    verifyCompletion(testContext, action, invocation -> {
      assertEquals(Status.EXCEPTION, invocation.getStatus());
      assertEquals(exception, invocation.getError());
    });
  }

  @Test
  @DisplayName("Expect ActionInvocation with EXCEPTION status when action is null")
  void actionNull(VertxTestContext testContext) {
    verifyCompletion(testContext, null, invocation -> {
      assertEquals(Status.EXCEPTION, invocation.getStatus());
      assertEquals(IllegalStateException.class, invocation.getError().getClass());
    });
  }

  void verifyCompletion(VertxTestContext testContext, Action action,
      Consumer<ActionInvocation> assertions) {
    ActionInvoker.rxApply(action, someContext())
        .subscribe(result -> testContext.verify(() -> {
          assertions.accept(result);
          testContext.completeNow();
        }), testContext::failNow);
  }

}
