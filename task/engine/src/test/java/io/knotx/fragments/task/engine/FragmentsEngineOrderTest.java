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
package io.knotx.fragments.task.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentOperation;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.api.SyncFragmentOperation;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.Task;
import io.knotx.junit5.util.RequestUtil;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class FragmentsEngineOrderTest {

  private static final FragmentOperation TIME_CONSUMING_OPERATION =
      (SyncFragmentOperation) context -> {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        return FragmentResult.success(context.getFragment());
      };
  private static final FragmentOperation SIMPLE_OPERATION =
      (SyncFragmentOperation) fragmentContext -> FragmentResult
          .success(fragmentContext.getFragment());

  @Test
  @DisplayName("Expect fragments in incoming order")
  void expectCorrectOrder(VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    // given
    List<FragmentContextTaskAware> events = Arrays.asList(
        initFragmentEventContextTaskAware("first fragment", TIME_CONSUMING_OPERATION),
        initFragmentEventContextTaskAware("second fragment", SIMPLE_OPERATION)
    );

    // when
    Single<List<TaskResult>> result = new FragmentsEngine(vertx).execute(events);

    // then
    verifyExecution(result, fragmentEvents -> testContext.verify(() -> {
      assertEquals(2, fragmentEvents.size());
      assertEquals("first fragment", fragmentEvents.get(0).getFragment().getBody());
      assertEquals("second fragment", fragmentEvents.get(1).getFragment().getBody());
    }), testContext);
  }

  private FragmentContextTaskAware initFragmentEventContextTaskAware(String fragmentBody,
      FragmentOperation operation) {
    Node graphNode = Nodes.single("id", operation);
    Fragment fragment = new Fragment("snippet", new JsonObject(), fragmentBody);

    return new FragmentContextTaskAware(new Task("task", graphNode), new ClientRequest(), fragment);
  }

  void verifyExecution(Single<List<TaskResult>> result,
      Consumer<List<TaskResult>> successConsumer,
      VertxTestContext testContext) throws Throwable {
    RequestUtil.subscribeToResult_shouldSucceed(testContext, result, successConsumer);
    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }
}
