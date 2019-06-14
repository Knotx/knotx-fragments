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
package io.knotx.fragments.handler;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Stubber;

import com.google.common.collect.Lists;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.handler.options.FragmentsHandlerOptions;
import io.knotx.junit5.util.FileReader;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.api.context.RequestContext;
import io.knotx.server.api.context.RequestEvent;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FragmentsHandlerTest {

  @Test
  @DisplayName("Expect fail with Status Code 500 when any fragment is failed.")
  void shouldFail(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    //given
    RoutingContext routingContext = mockRoutingContext("failing-task");

    FragmentsHandler underTest = new FragmentsHandler(vertx,
        from("tasks/fragment-handler/failingAction.json"));

    //when
    underTest.handle(routingContext);

    //then
    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    })
        .when(routingContext)
        .fail(500);

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));

    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }

  }

  @Test
  @DisplayName("Expect continuing processing next handler when task completes on _success.")
  void shouldSuccess(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    //given
    RoutingContext routingContext = mockRoutingContext("success-task");

    FragmentsHandler underTest = new FragmentsHandler(vertx,
        from("tasks/fragment-handler/successAction.json"));

    //when
    underTest.handle(routingContext);

    //then
    doAnswer(invocation -> {
      testContext.completeNow();
      return null;
    })
        .when(routingContext)
        .next();

    assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));

    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private RoutingContext mockRoutingContext(String task) {
    RequestContext requestContext = new RequestContext(
        new RequestEvent(new ClientRequest(), new JsonObject()));

    RoutingContext routingContext = Mockito.mock(RoutingContext.class);

    when(routingContext.get(eq(RequestContext.KEY))).thenReturn(requestContext);
    when(routingContext.get(eq("fragments"))).thenReturn(
        newArrayList(fragment(task)));
    return routingContext;
  }

  private Fragment fragment(String task) {
    return new Fragment("type",
        new JsonObject().put(FragmentsHandlerOptions.DEFAULT_TASK_KEY, task), "");
  }

  private JsonObject from(String fileName) throws IOException {
    return new JsonObject(FileReader.readText(fileName));
  }
}
