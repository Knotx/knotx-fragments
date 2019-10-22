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
package io.knotx.fragments.task.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class TaskOptionsTest {

  @Test
  @DisplayName("Expect default Task provider when factory not defined")
  void expectDefaultTaskProvider(Vertx vertx) throws Throwable {
    verify("tasks/defaultTaskProvider.conf", config -> {
      TaskOptions options = new TaskOptions(config);
      assertEquals("default", options.getFactory().getName());
      assertTrue(options.getFactory().getConfig().isEmpty());
    }, vertx);
  }

  @Test
  @DisplayName("Expect custom Task provider with configuration.")
  void expectCustomTaskProvider(Vertx vertx) throws Throwable {
    verify("tasks/customTaskProvider.conf", config -> {
      TaskOptions options = new TaskOptions(config);
      assertEquals("custom", options.getFactory().getName());
      assertEquals(new JsonObject().put("anyKey", "anyValue"), options.getFactory().getConfig());
    }, vertx);
  }

  @Test
  @DisplayName("Expect task with action node when action defined directly in the task.")
  void expectActionNodeWhenActionDirectlyDefined(Vertx vertx) throws Throwable {
    validateActionNode("tasks/taskWithActionNode.conf", vertx);
  }

  @Test
  @DisplayName("Expect task with Action node when action is configured.")
  void expectActionNodeWhenActionDefined(Vertx vertx) throws Throwable {
    validateActionNode("tasks/taskWithActionNode-fullSyntax.conf", vertx);
  }

  @Test
  @DisplayName("Expect sub-tasks node when actions is configured")
  void expectSubTaskNodeWhenActionsDefined(Vertx vertx) throws Throwable {
    validateSubTasksNode("tasks/taskWithSubTasksDeprecated.conf", vertx);
  }

  @Test
  @DisplayName("Expect sub-tasks node when subTasks directly is configured")
  void expectSubTasksNodeWhenSubTasksDirectlyDefined(Vertx vertx) throws Throwable {
    validateSubTasksNode("tasks/taskWithSubTasks.conf", vertx);
  }

  @Test
  @DisplayName("Expect sub-tasks node when subtasks directly is configured")
  void expectSubTasksNodeWhenSubTasksDirectlyDefinedWithTypo(Vertx vertx) throws Throwable {
    validateSubTasksNode("tasks/taskWithSubTasks-typo.conf", vertx);
  }

  @Test
  @DisplayName("Expect sub-tasks node when subTasks is configured")
  void expectSubTasksNodeWhenSubTasksDefined(Vertx vertx) throws Throwable {
    validateSubTasksNode("tasks/taskWithSubTasks-fullSyntax.conf", vertx);
  }

  @Test
  @DisplayName("Expect sub-tasks node when subtasks is configured")
  void expectSubTasksNodeWhenSubtasksDefined(Vertx vertx) throws Throwable {
    validateSubTasksNode("tasks/taskWithSubTasks-fullSyntax-typo.conf", vertx);
  }

  @Test
  @DisplayName("Expect nodes configured with _success flow")
  void expectTransitionSuccessWithNodeBThenNodeC(Vertx vertx) throws Throwable {
    verify("tasks/taskWithTransitions.conf", config -> {
      GraphOptions graphOptions = new TaskOptions(config).getGraph();
      Optional<GraphOptions> nodeB = graphOptions.get("_success");
      assertTrue(nodeB.isPresent());
      assertEquals("b", getAction(nodeB.get()));
      Optional<GraphOptions> nodeC = nodeB.get().get("_success");
      assertTrue(nodeC.isPresent());
      assertEquals("c", getAction(nodeC.get()));
    }, vertx);
  }

  private void validateActionNode(String file, Vertx vertx) throws Throwable {
    verify(file, config -> {
      GraphOptions graphOptions = new TaskOptions(config).getGraph();
      assertEquals("a", getAction(graphOptions));
      assertEquals(GraphOptions.ACTION, graphOptions.getNode().getFactory());
    }, vertx);
  }

  private void validateSubTasksNode(String file, Vertx vertx) throws Throwable {
    verify(file, config -> {
      TaskOptions taskOptions = new TaskOptions(config);
      GraphOptions graphOptions = taskOptions.getGraph();
      assertEquals(GraphOptions.SUBTASKS, graphOptions.getNode().getFactory());
      SubTasksNodeConfigOptions subTasks = new SubTasksNodeConfigOptions(
          graphOptions.getNode().getConfig());
      assertEquals(2, subTasks.getSubTasks().size());
    }, vertx);
  }

  private String getAction(GraphOptions graphOptions) {
    return new ActionNodeConfigOptions(
        graphOptions.getNode().getConfig()).getAction();
  }

  void verify(String fileName, Consumer<JsonObject> assertions, Vertx vertx) throws Throwable {
    VertxTestContext testContext = new VertxTestContext();
    Handler<AsyncResult<JsonObject>> configHandler = testContext
        .succeeding(config -> testContext.verify(() -> {
          assertions.accept(config);
          testContext.completeNow();
        }));
    fromHOCON(fileName, vertx, configHandler);

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private void fromHOCON(String fileName, Vertx vertx,
      Handler<AsyncResult<JsonObject>> configHandler) {
    ConfigRetrieverOptions options = new ConfigRetrieverOptions();
    options.addStore(new ConfigStoreOptions()
        .setType("file")
        .setFormat("hocon")
        .setConfig(new JsonObject().put("path", fileName)));

    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    retriever.getConfig(configHandler);
  }

}