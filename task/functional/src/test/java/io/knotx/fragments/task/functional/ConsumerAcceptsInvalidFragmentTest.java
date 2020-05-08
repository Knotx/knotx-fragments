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
package io.knotx.fragments.task.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.task.handler.FragmentsHandlerFactory;
import io.knotx.fragments.task.handler.log.api.model.FragmentExecutionLog;
import io.knotx.fragments.task.handler.log.api.model.FragmentExecutionLog.ExecutionStatus;
import io.knotx.fragments.task.handler.log.api.model.GraphNodeExecutionLog;
import io.knotx.fragments.task.handler.log.api.model.LoggedNodeStatus;
import io.knotx.fragments.task.factory.generic.DefaultTaskFactoryConfig;
import io.knotx.fragments.task.functional.utils.RoutingContextStub;
import io.knotx.junit5.util.HoconLoader;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  @DisplayName("Expect invalid HTML fragment with single node task contains debug data.")
  void invalidHTMLFragmentWithSingleNodeTask(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    String configFile = "conf/invalid-single-node-task-with-consumer.conf";
    Fragment fragment = fragment("snippet", "failing-single-node-task", "<div>some content</div>");
    Consumer<FragmentExecutionLog> assertions = executionLog -> {
      GraphNodeExecutionLog graphRootNode = executionLog.getGraph();
      checkTimelines(graphRootNode);
      checkMissingNode(graphRootNode);
    };

    verifyDebugData(configFile, fragment, HTML_BODY_TO_EXECUTION_LOG, assertions, vertx,
        testContext);
  }

  @Test
  @DisplayName("Expect invalid HTML fragment with composite node task contains debug data.")
  void invalidHTMLFragmentWithCompositeNodeTask(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    String configFile = "conf/invalid-composite-node-task-with-consumer.conf";
    Fragment fragment = fragment("snippet", "failing-composite-node-task",
        "<div>some content</div>");

    verifyDebugData(configFile, fragment, HTML_BODY_TO_EXECUTION_LOG, COMPOSITE_ASSERTIONS, vertx,
        testContext);
  }

  @Test
  @DisplayName("Expect invalid JSON fragment with single node task contains debug data.")
  void invalidJsonFragmentWithSingleNodeTask(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    String configFile = "conf/invalid-single-node-task-with-consumer.conf";
    Fragment fragment = fragment("json", "failing-single-node-task", "{}");
    Consumer<FragmentExecutionLog> assertions = executionLog -> {
      GraphNodeExecutionLog graphRootNode = executionLog.getGraph();
      checkTimelines(graphRootNode);
      checkMissingNode(graphRootNode);
    };

    verifyDebugData(configFile, fragment, JSON_BODY_TO_EXECUTION_LOG, assertions, vertx,
        testContext);
  }

  @Test
  @DisplayName("Expect invalid JSON fragment with composite node task contains debug data.")
  void invalidJsonFragmentWithCompositeNodeTask(Vertx vertx, VertxTestContext testContext)
      throws Throwable {
    String configFile = "conf/invalid-composite-node-task-with-consumer.conf";
    Fragment fragment = fragment("json", "failing-composite-node-task", "{}");

    verifyDebugData(configFile, fragment, JSON_BODY_TO_EXECUTION_LOG, COMPOSITE_ASSERTIONS, vertx,
        testContext);
  }

  private static final Consumer<FragmentExecutionLog> COMPOSITE_ASSERTIONS =
      executionLog -> {
        GraphNodeExecutionLog graphRootNode = executionLog.getGraph();

        assertEquals(2, graphRootNode.getSubtasks().size());
        GraphNodeExecutionLog successSubtask = graphRootNode.getSubtask("success-action")
            .orElseThrow(() -> new AssertionError("Subtask node not found!"));
        checkTimelines(successSubtask);
        assertEquals(LoggedNodeStatus.SUCCESS, successSubtask.getStatus());

        GraphNodeExecutionLog failingSubtask = graphRootNode.getSubtask("failing-action")
            .orElseThrow(() -> new AssertionError("Subtask node not found!"));
        checkTimelines(failingSubtask);
        assertEquals(LoggedNodeStatus.ERROR, failingSubtask.getStatus());

        checkMissingNode(graphRootNode);
      };


  private static final Function<String, FragmentExecutionLog> JSON_BODY_TO_EXECUTION_LOG = fragmentBody -> {
    assertTrue(new JsonObject(fragmentBody).containsKey("_knotx_fragment"));
    JsonObject debugData = new JsonObject(fragmentBody).getJsonObject("_knotx_fragment");
    return new FragmentExecutionLog(debugData);
  };

  private static final Function<String, FragmentExecutionLog> HTML_BODY_TO_EXECUTION_LOG = fragmentBody -> {
    String scriptRegexp =
        "<script data-knotx-debug=\"log\" data-knotx-id=\".*\" type=\"application/json\">(?<fragmentEventJson>.*?)</script>";
    Pattern scriptPattern = Pattern.compile(scriptRegexp, Pattern.DOTALL);

    // then
    Matcher matcher = scriptPattern.matcher(fragmentBody);
    assertTrue(matcher.find());
    return new FragmentExecutionLog(new JsonObject(matcher.group("fragmentEventJson")));
  };

  private static final Consumer<FragmentExecutionLog> FRAGMENT_ASSERTIONS = executionLog -> {
    assertEquals(ExecutionStatus.FAILURE, executionLog.getStatus());
    assertNotEquals(0, executionLog.getStartTime(),
        "Fragment processing start time should be set.");
    assertNotEquals(0, executionLog.getFinishTime(),
        "Fragment processing end time should be set.");
    GraphNodeExecutionLog graphRootNode = executionLog.getGraph();
    assertNotNull(graphRootNode);
    assertEquals(LoggedNodeStatus.ERROR, graphRootNode.getStatus());
  };

  private void verifyDebugData(String configFile, Fragment fragment,
      Function<String, FragmentExecutionLog> toExecutionLog,
      Consumer<FragmentExecutionLog> assertions,
      Vertx vertx, VertxTestContext testContext) throws Throwable {
    HoconLoader.verifyAsync(configFile, loadedConfig -> {
      // checkpoints
      Checkpoint updateFragmentCheckpoint = testContext.checkpoint();
      Checkpoint callNextHandlerCheckpoint = testContext.checkpoint();

      //given
      RoutingContext routingContextMock = RoutingContextStub
          .create(fragment, Collections.singletonMap("Allow-Invalid-Fragments", "true"),
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
          FragmentExecutionLog executionLog = toExecutionLog.apply(fragmentBody);
          FRAGMENT_ASSERTIONS.accept(executionLog);
          assertions.accept(executionLog);
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
          .create(vertx, loadedConfig);

      //when
      underTest.handle(routingContextMock);
    }, testContext, vertx);
  }

  private static void checkTimelines(GraphNodeExecutionLog graphNode) {
    assertNotEquals(0, graphNode.getStarted(),
        "Node processing start time should be set.");
    assertNotEquals(0, graphNode.getFinished(),
        "Node processing end time should be set.");
  }

  private static void checkMissingNode(GraphNodeExecutionLog graphNode) {
    assertTrue(graphNode.getOn().containsKey("_error"), "Missing node exists");
    GraphNodeExecutionLog missingNode = graphNode.getOn().get("_error");
    assertEquals(LoggedNodeStatus.MISSING, missingNode.getStatus());
  }

  private static Fragment fragment(String type, String task, String body) {
    return new Fragment(type,
        new JsonObject().put(DefaultTaskFactoryConfig.DEFAULT_TASK_NAME_KEY, task), body);
  }
}
