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
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;

public final class TestUtils {

  static final Fragment FRAGMENT = new Fragment("", new JsonObject(), "");
  static final FragmentContext FRAGMENT_CONTEXT = new FragmentContext(FRAGMENT, new ClientRequest());
  static final FragmentResult FRAGMENT_RESULT = FragmentResult.success(FRAGMENT);

  private TestUtils() {
    // utility class
  }

  static void assertSuccessDelivered(VertxTestContext testContext, FragmentOperation operation)
      throws InterruptedException {
    callOperation(testContext, operation, asyncResult -> {
      assertTrue(asyncResult.succeeded());
      testContext.completeNow();
    });
  }

  static void assertFailureDelivered(VertxTestContext testContext, FragmentOperation operation)
      throws InterruptedException {
    callOperation(testContext, operation, asyncResult -> {
      assertTrue(asyncResult.failed());
      testContext.completeNow();
    });
  }

  static void assertFailureDelivered(VertxTestContext testContext, FragmentOperation operation, Class<? extends Throwable> exceptionClass)
      throws InterruptedException {
    callOperation(testContext, operation, asyncResult -> {
      assertTrue(asyncResult.failed());
      assertEquals(exceptionClass, asyncResult.cause().getClass());
      testContext.completeNow();
    });
  }

  private static void callOperation(VertxTestContext testContext, FragmentOperation operation,
                                    Handler<AsyncResult<FragmentResult>> assertions) throws InterruptedException {
    operation.apply(FRAGMENT_CONTEXT, assertions);
    testContext.awaitCompletion(1000, TimeUnit.MILLISECONDS);
  }

}
