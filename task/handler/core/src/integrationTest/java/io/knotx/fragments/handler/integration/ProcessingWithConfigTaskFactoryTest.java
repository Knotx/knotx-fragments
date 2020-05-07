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
package io.knotx.fragments.handler.integration;

import static org.mockito.Mockito.doAnswer;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.FragmentsHandlerFactory;
import io.knotx.fragments.task.factory.config.DefaultTaskFactoryConfig;
import io.knotx.fragments.handler.utils.RoutingContextStub;
import io.knotx.junit5.util.HoconLoader;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProcessingWithConfigTaskFactoryTest {

  private static final String CUSTOM_TASK_NAME_KEY = "task";
  private static final String EMPTY_BODY = "";

  @Test
  @DisplayName("Expect continuing processing next handler when no fragment is failed.")
  void shouldSuccess(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verifyAsync("conf/config-task-factory-with-success-task.conf", config -> {
      //given
      RoutingContext routingContext = mockRoutingContext("success-task");
      Handler<RoutingContext> underTest = new FragmentsHandlerFactory().create(vertx, config);

      //when
      underTest.handle(routingContext);

      //then
      doAnswer(invocation -> {
        testContext.completeNow();
        return null;
      })
          .when(routingContext)
          .next();
    }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect fail with Status Code 500 when any fragment is failed.")
  void shouldFail(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verifyAsync("conf/config-task-factory-with-failing-task.conf", config -> {
      // given
      RoutingContext routingContext = mockRoutingContext("failing-task");
      Handler<RoutingContext> underTest = new FragmentsHandlerFactory().create(vertx, config);

      // when
      doAnswer(invocation -> {
        testContext.completeNow();
        return null;
      })
          .when(routingContext)
          .fail(500);

      underTest.handle(routingContext);

      // then verified as correct (assertion inside HoconLoader::verify)
    }, testContext, vertx);
  }

  private RoutingContext mockRoutingContext(String task) {
    return RoutingContextStub
        .create(fragment(task), Collections.emptyMap(), Collections.emptyMap());
  }

  private RoutingContext mockRoutingContext(String task, Map<String, String> headers,
      Map<String, String> params) {
    return RoutingContextStub.create(fragment(task), headers, params);
  }

  private Fragment fragment(String task) {
    return new Fragment("type",
        new JsonObject().put(DefaultTaskFactoryConfig.DEFAULT_TASK_NAME_KEY, task), "");
  }
}
