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

import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.FragmentEventContextTaskAware;
import io.knotx.fragments.handler.api.exception.ConfigurationException;
import io.knotx.fragments.HoconLoader;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.task.factory.DefaultTaskFactoryConfig;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.api.context.RequestContext;
import io.knotx.server.api.context.RequestEvent;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FragmentsHandlerTest {

  private static final String CUSTOM_TASK_NAME_KEY = "task";
  private static final String EMPTY_BODY = "";

  @Test
  @DisplayName("Expect continuing processing next handler when no fragment is failed.")
  void shouldSuccess(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verify("handler/singleTaskFactoryWithSuccessTask.conf", config -> {
      //given
      RoutingContext routingContext = mockRoutingContext("success-task");
      FragmentsHandler underTest = new FragmentsHandler(vertx, config);

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
    HoconLoader.verify("handler/singleTaskFactoryWithFailingTask.conf", config -> {
      //given
      RoutingContext routingContext = mockRoutingContext("failing-task");
      FragmentsHandler underTest = new FragmentsHandler(vertx, config);

      //when
      underTest.handle(routingContext);

      //then
      doAnswer(invocation -> {
        testContext.completeNow();
        return null;
      })
          .when(routingContext)
          .fail(500);
    }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect marked fragment when HTML fragment body writer configured.")
  void snippetFragmentWithHtmlConsumer(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verify("handler/fragmentWithHtmlConsumer.conf", config -> {
      //given
      Fragment fragment = new Fragment("snippet",
          new JsonObject().put(DefaultTaskFactoryConfig.DEFAULT_TASK_NAME_KEY, "success-task"),
          EMPTY_BODY);

      // when
      Single<List<FragmentEventContextTaskAware>> rxDoHandle = new FragmentsHandler(vertx, config)
          .doHandle(Collections.singletonList(fragment), new ClientRequest());

      rxDoHandle.subscribe(
          result -> testContext.verify(() -> {
            // then
            Optional<FragmentEvent> event = result.stream().findFirst()
                .map(FragmentEventContextTaskAware::getFragmentEventContext)
                .map(FragmentEventContext::getFragmentEvent);
            assertTrue(event.isPresent());
            String body = event.get().getFragment().getBody();
            assertTrue(body.contains("<!-- data-knotx-id="));
            testContext.completeNow();
          }),
          testContext::failNow
      );
    }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect processed fragment when factory accepts fragment.")
  void taskFactoryWithTaskEndingWithSuccess(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verify("handler/fragmentWithSuccessTask.conf", config -> {
      //given
      FragmentsHandler underTest = new FragmentsHandler(vertx, config);
      Fragment fragment = new Fragment("type",
          new JsonObject().put(DefaultTaskFactoryConfig.DEFAULT_TASK_NAME_KEY, "success-task"),
          EMPTY_BODY);
      String expectedBody = "success";

      //when
      Single<List<FragmentEventContextTaskAware>> rxDoHandle = underTest
          .doHandle(Collections.singletonList(fragment), new ClientRequest());

      rxDoHandle.subscribe(
          result -> testContext.verify(() -> {
            // then
            FragmentEvent fragmentEvent = result.get(0).getFragmentEventContext().getFragmentEvent();
            assertEquals(Status.SUCCESS, fragmentEvent.getStatus());
            assertEquals(expectedBody, fragmentEvent.getFragment().getBody());
            testContext.completeNow();
          }),
          testContext::failNow
      );
    }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect unprocessed fragment when all factories do not accept fragment.")
  void singleFactoryNotAcceptingFragment(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verify("handler/singleTaskFactoryWithSuccessTask.conf", config -> {
      //given
      FragmentsHandler underTest = new FragmentsHandler(vertx, config);
      Fragment fragment = new Fragment("type", new JsonObject(), EMPTY_BODY);

      //when
      Single<List<FragmentEventContextTaskAware>> rxDoHandle = underTest
          .doHandle(Collections.singletonList(fragment), new ClientRequest());

      rxDoHandle.subscribe(
          result -> testContext.verify(() -> {
            // then
            assertEquals(Status.UNPROCESSED, result.get(0).getFragmentEventContext().getFragmentEvent().getStatus());
            testContext.completeNow();
          }),
          testContext::failNow
      );
    }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect processed fragment when second factory accepts fragment.")
  void twoFactoriesWithTheSameName(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verify("handler/manyTaskFactoriesWithTheSameName.conf", config -> {
      //given
      FragmentsHandler underTest = new FragmentsHandler(vertx, config);
      Fragment fragment = new Fragment("type",
          new JsonObject().put(CUSTOM_TASK_NAME_KEY, "success-task"), EMPTY_BODY);
      String expectedBody = "custom";

      //when
      Single<List<FragmentEventContextTaskAware>> rxDoHandle = underTest
          .doHandle(Collections.singletonList(fragment), new ClientRequest());

      rxDoHandle.subscribe(
          result -> testContext.verify(() -> {
            assertEquals(expectedBody, result.get(0).getFragmentEventContext().getFragmentEvent().getFragment().getBody());
            testContext.completeNow();
          }),
          testContext::failNow
      );
    }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect empty task factory and consumer factory when not configured.")
  void tasksAndConsumersNotDefined(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verify("handler/factoriesNotDefined.conf", config -> {
      //given
      new FragmentsHandler(vertx, config);
      // no exception
      testContext.completed();
    }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect exception when task factory name is not defined")
  void taskFactoryNameNotDefined(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verify("handler/taskFactoryNameNotDefined.conf", config -> {
      //given
      try {
        new FragmentsHandler(vertx, config);
      } catch (ConfigurationException e) {
        testContext.completed();
      }
    }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect exception when consumer factory name is not defined")
  void consumerFactoryNameNotDefined(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verify("handler/consumerFactoryNameNotDefined.conf", config -> {
      //given
      try {
        new FragmentsHandler(vertx, config);
      } catch (ConfigurationException e) {
        testContext.completed();
      }
    }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect exception when task factory name is not found")
  void taskFactoryNotFound(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verify("handler/taskFactoryNotFound.conf", config -> {
      //given
      try {
        new FragmentsHandler(vertx, config);
      } catch (ConfigurationException e) {
        assertTrue(e.getMessage().contains("Task factory"));
        testContext.completed();
      }
    }, testContext, vertx);
  }

  @Test
  @DisplayName("Expect exception when consumer factory name is not found")
  void consumerFactoryNotFound(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    HoconLoader.verify("handler/consumerFactoryNotFound.conf", config -> {
      //given
      try {
        new FragmentsHandler(vertx, config);
      } catch (ConfigurationException e) {
        assertTrue(e.getMessage().contains("Consumer factory"));
        testContext.completed();
      }
    }, testContext, vertx);
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
        new JsonObject().put(DefaultTaskFactoryConfig.DEFAULT_TASK_NAME_KEY, task), "");
  }
}
