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

import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.junit5.KnotxExtension;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

@ExtendWith(KnotxExtension.class)
class SyncFragmentOperationTest {

  private static final Fragment FRAGMENT = new Fragment("", new JsonObject(), "");
  private static final FragmentContext FRAGMENT_CONTEXT = new FragmentContext(FRAGMENT, new ClientRequest());

  @Test
  @DisplayName("Expect succeeded AsyncResult when operation succeeds")
  void succeedingOperation(VertxTestContext testContext) throws InterruptedException {
    SyncFragmentOperation tested = fragmentContext -> new FragmentResult(FRAGMENT, SUCCESS_TRANSITION);

    callOperation(testContext, tested, asyncResult -> {
      assertTrue(asyncResult.succeeded());
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Expect succeeded AsyncResult when operation returns null")
  void nullReturningOperation(VertxTestContext testContext) throws InterruptedException {
    SyncFragmentOperation tested = fragmentContext -> null;

    callOperation(testContext, tested, asyncResult -> {
      assertTrue(asyncResult.succeeded());
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Expect failed AsyncResult when operation throws")
  void throwingOperation(VertxTestContext testContext) throws InterruptedException {
    RuntimeException exception = new RuntimeException();
    SyncFragmentOperation tested = fragmentContext -> {
      throw exception;
    };

    callOperation(testContext, tested, asyncResult -> {
      assertTrue(asyncResult.failed());
      assertEquals(asyncResult.cause(), exception);
      testContext.completeNow();
    });

  }

  private void callOperation(VertxTestContext testContext, FragmentOperation operation,
                             Handler<AsyncResult<FragmentResult>> assertions) throws InterruptedException {
    operation.apply(FRAGMENT_CONTEXT, assertions);
    testContext.awaitCompletion(1000, TimeUnit.MILLISECONDS);
  }


}
