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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.api.FragmentEvent;
import io.knotx.fragments.engine.api.FragmentEvent.Status;
import io.knotx.fragments.handler.FragmentsHandlerFactory;
import io.knotx.fragments.handler.consumer.api.FragmentExecutionLogConsumer;
import io.knotx.fragments.handler.consumer.api.model.FragmentExecutionLog;
import io.knotx.fragments.handler.consumer.api.model.GraphNodeExecutionLog;
import io.knotx.fragments.handler.consumer.api.model.LoggedNodeStatus;
import io.knotx.fragments.task.factory.DefaultTaskFactoryConfig;
import io.knotx.fragments.utils.HoconLoader;
import io.knotx.fragments.utils.RoutingContextMock;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
class ConsumerAcceptsInvalidFragmentTest {

  @Test
  @DisplayName("Expect invalid JSON fragment with debug data.")
  void invalidJsonFragmentWithDebugData(Vertx vertx, VertxTestContext testContext) throws Throwable {
    HoconLoader.verifyAsync("conf/invalid-json-fragment-with-consumer.conf", config -> {
      // checkpoints
      Checkpoint updateFragmentCheckpoint = testContext.checkpoint();
      Checkpoint callNextHandlerCheckpoint = testContext.checkpoint();

      //given
      Fragment fragment = fragment("json", "failing-task", "{}");

      RoutingContext routingContextMock = RoutingContextMock.create(fragment, Collections.emptyMap(),
          Collections.singletonMap("debug", "true"));

      doAnswer(invocation -> {
        String key = invocation.getArgument(0);
        if ("fragments".equals(key)) {
          // then
          List<Fragment> fragmentList = invocation.getArgument(1);
          assertNotNull(fragmentList);
          assertEquals(1, fragmentList.size());
          String fragmentBody = fragmentList.get(0).getBody();
          assertNotNull(fragmentBody);
          assertTrue(new JsonObject(fragmentBody).containsKey("_knotx_fragment"));
          JsonObject debugData = new JsonObject(fragmentBody).getJsonObject("_knotx_fragment");

          // verify node fragment processing
          FragmentExecutionLog executionLog = new FragmentExecutionLog(debugData);
          assertNotEquals(0, executionLog.getStartTime(), "Fragment processing start time should be set.");
          assertNotEquals(0, executionLog.getFinishTime(), "Fragment processing end time should be set.");
          assertEquals(Status.FAILURE, executionLog.getStatus());

          // verify node processing
          GraphNodeExecutionLog graphRootNode = executionLog.getGraph();
          assertNotNull(graphRootNode);
          assertNotEquals(0, graphRootNode.getStarted(), "Node processing start time should be set.");
          assertNotEquals(0, graphRootNode.getFinished(), "Node processing end time should be set.");
          assertEquals(LoggedNodeStatus.ERROR, graphRootNode.getStatus());

          // verify missing node
          assertTrue(graphRootNode.getOn().containsKey("_error"), "Missing node exists");
          GraphNodeExecutionLog missingNode = graphRootNode.getOn().get("_error");
          assertEquals(LoggedNodeStatus.MISSING, missingNode.getStatus());

          updateFragmentCheckpoint.flag();
        }
        return routingContextMock;
      })
          .when(routingContextMock)
          .put(Mockito.any(), Mockito.any());

      doAnswer(invocation -> {
        callNextHandlerCheckpoint.flag();
        return routingContextMock;
      })
          .when(routingContextMock)
          .next();

      Handler<RoutingContext> underTest = new FragmentsHandlerFactory()
          .create(vertx, config);

      //when
      underTest.handle(routingContextMock);
    }, testContext, vertx);
  }

  private Fragment fragment(String type, String task, String body) {
    return new Fragment(type,
        new JsonObject().put(DefaultTaskFactoryConfig.DEFAULT_TASK_NAME_KEY, task), body);
  }
}
