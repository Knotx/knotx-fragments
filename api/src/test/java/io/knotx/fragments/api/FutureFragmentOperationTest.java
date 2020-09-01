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

import static io.knotx.fragments.api.TestUtils.FRAGMENT_RESULT;
import static io.knotx.fragments.api.TestUtils.assertFailureDelivered;
import static io.knotx.fragments.api.TestUtils.assertSuccessDelivered;

import io.knotx.junit5.KnotxExtension;
import io.vertx.core.Future;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(KnotxExtension.class)
class FutureFragmentOperationTest {

  @Test
  @DisplayName("Expect succeeded AsyncResult when operation returns succeeded future")
  void succeedingOperation(VertxTestContext testContext) throws InterruptedException {
    FutureFragmentOperation tested = fragmentContext -> Future.succeededFuture(FRAGMENT_RESULT);

    assertSuccessDelivered(testContext, tested);
  }

  @Test
  @DisplayName("Expect failed AsyncResult when operation returns null future")
  void nullOperation(VertxTestContext testContext) throws InterruptedException {
    FutureFragmentOperation tested = fragmentContext -> null;

    assertFailureDelivered(testContext, tested, IllegalStateException.class);
  }

  @Test
  @DisplayName("Expect failed AsyncResult when operation returns failed future")
  void failedFutureOperation(VertxTestContext testContext) throws InterruptedException {
    FutureFragmentOperation tested = fragmentContext -> Future.failedFuture(new RuntimeException());

    assertFailureDelivered(testContext, tested, RuntimeException.class);
  }

  @Test
  @DisplayName("Expect failed AsyncResult when operation throws")
  void throwingOperation(VertxTestContext testContext) throws InterruptedException {
    FutureFragmentOperation tested = fragmentContext -> {
      throw new RuntimeException();
    };

    assertFailureDelivered(testContext, tested, RuntimeException.class);
  }

}
