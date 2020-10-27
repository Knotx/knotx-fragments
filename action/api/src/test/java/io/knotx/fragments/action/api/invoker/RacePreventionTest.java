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

import static io.knotx.fragments.action.api.invoker.TestUtils.actionReturning;
import static io.knotx.fragments.action.api.invoker.TestUtils.successResult;
import static io.knotx.fragments.action.api.invoker.TestUtils.verifyExecution;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, timeUnit = SECONDS)
class RacePreventionTest {

  @Test
  @DisplayName("Expect failed AsyncResult when given null AsyncResult")
  void nullAsyncResult(VertxTestContext testContext) {
    Action tested = RacePrevention.wrap(actionReturning(null));

    verifyExecution(testContext, tested,
        asyncResult -> assertEquals(IllegalStateException.class, asyncResult.cause().getClass()));
  }

  @Test
  @DisplayName("Expect propagated AsyncResult when given succeeded AsyncResult")
  void succeededResult(VertxTestContext testContext) {
    AsyncResult<FragmentResult> original = Future.succeededFuture();
    Action tested = RacePrevention.wrap(actionReturning(original));

    verifyExecution(testContext, tested, asyncResult -> assertEquals(original, asyncResult));
  }

  @Test
  @DisplayName("Expect propagated AsyncResult when given failed AsyncResult")
  void failedResult(VertxTestContext testContext) {
    AsyncResult<FragmentResult> original = Future.failedFuture(new RuntimeException());
    Action tested = RacePrevention.wrap(actionReturning(original));

    verifyExecution(testContext, tested, asyncResult -> assertEquals(original, asyncResult));
  }

  @Test
  @DisplayName("Expect completion when given not-yet-resolved, completing Future")
  void lateSucceedingFuture(VertxTestContext testContext) {
    FragmentResult result = successResult();
    Promise<FragmentResult> promise = Promise.promise();
    Action tested = RacePrevention.wrap(actionReturning(promise.future()));

    // Action is called, handler is called with future that did not yet complete
    verifyExecution(testContext, tested, asyncResult -> assertEquals(result, asyncResult.result()));

    // We complete the promise after original action's handler was called
    // This is ensured because the original handler is called synchronously within this test
    promise.complete(result);
  }

  @Test
  @DisplayName("Expect completion when given not-yet-resolved, failing Future")
  void lateFailingFuture(VertxTestContext testContext) {
    RuntimeException exception = new RuntimeException();
    Promise<FragmentResult> promise = Promise.promise();
    Action tested = RacePrevention.wrap(actionReturning(promise.future()));

    // Action is called, handler is called with future that did not yet complete
    verifyExecution(testContext, tested,
        asyncResult -> assertEquals(exception, asyncResult.cause()));

    // We fail the promise after original action's handler was called
    // This is ensured because the original handler is called synchronously within this test
    promise.fail(exception);
  }

  @Test
  @DisplayName("Expect failed AsyncResult when given not-yet-resolved AsyncResult that is not a Future")
  void lateAsyncResult(VertxTestContext testContext) {
    Action tested = RacePrevention.wrap(actionReturning(customNotFinishedAsyncResult()));

    verifyExecution(testContext, tested,
        asyncResult -> assertEquals(IllegalStateException.class, asyncResult.cause().getClass()));
  }

  private static AsyncResult<FragmentResult> customNotFinishedAsyncResult() {
    return new AsyncResult<FragmentResult>() {
      @Override
      public FragmentResult result() {
        return null;
      }

      @Override
      public Throwable cause() {
        return null;
      }

      @Override
      public boolean succeeded() {
        return false;
      }

      @Override
      public boolean failed() {
        return false;
      }
    };
  }

}
