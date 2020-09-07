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
package io.knotx.fragments.action.library;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.SyncAction;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class TestUtils {

  public static final String ACTION_ALIAS = "action";

  private TestUtils() {
    // Utility class
  }

  public static Fragment someFragment() {
    return new Fragment("type", new JsonObject(), "body");
  }

  public static Fragment someFragmentWithPayload(JsonObject payload) {
    return someFragment().mergeInPayload(payload);
  }

  public static FragmentContext someContext() {
    return someContext(someFragment());
  }

  public static FragmentContext someContext(Fragment fragment) {
    return new FragmentContext(fragment, new ClientRequest());
  }

  public static FragmentResult successResult() {
    return FragmentResult.success(someFragment());
  }

  public static FragmentResult failedResult() {
    return FragmentResult.fail(someFragment(), new RuntimeException());
  }

  public static Action doActionIdle() {
    return (SyncAction) fragmentContext -> FragmentResult.success(fragmentContext.getFragment());
  }

  public static void verifyActionResult(VertxTestContext testContext, Action action,
      Consumer<AsyncResult<FragmentResult>> assertions) {
    verifyActionResult(testContext, action, someFragment(), assertions);
  }

  public static void verifyActionResult(VertxTestContext testContext, Action action,
      Fragment fragment,
      Consumer<AsyncResult<FragmentResult>> assertions) {
    verifyActionResult(testContext, action, someContext(fragment), assertions);
  }

  public static void verifyActionResult(VertxTestContext testContext, Action action,
      FragmentContext input,
      Consumer<AsyncResult<FragmentResult>> assertions) {
    action.apply(input,
        result -> testContext.verify(() -> {
          assertions.accept(result);
          testContext.completeNow();
        }));
  }

  public static void verifyTwoActionResults(VertxTestContext testContext, Action action,
      FragmentContext firstContext, FragmentContext secondContext,
      BiConsumer<AsyncResult<FragmentResult>, AsyncResult<FragmentResult>> secondAssertions) {
    verifyActionResult(testContext, action, firstContext,
        firstResult -> verifyActionResult(testContext, action, secondContext, secondResult ->
            secondAssertions.accept(firstResult, secondResult)));
  }

}
