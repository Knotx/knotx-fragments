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
package io.knotx.fragments.handler.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.task.options.TaskOptions;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class TaskOptionsTest {

  @Test
  @DisplayName("Expect default Task builder when not defined")
  void expectDefaultTaskBuilder(Vertx vertx) throws Throwable {
    verify("tasks/taskWithSingleActionShort.conf", config -> {
      TaskOptions options = new TaskOptions(config);
      assertEquals("default", options.getFactory().getName());
      assertTrue(options.getFactory().getConfig().isEmpty());
    }, vertx);
  }

  @Test
  @DisplayName("Expect task with single action when action is directly configured")
  void expectActionNodeWhenActionDirectlyDefined(Vertx vertx) throws Throwable {
    verify("tasks/taskWithSingleActionShort.conf", config -> {
      NodeOptions nodeOptions = new TaskOptions(config).getConfig();
      assertEquals("simple", nodeOptions.getAction());
      assertFalse(nodeOptions.isComposite());
    }, vertx);
  }

  @Test
  @DisplayName("Expect composite node options when actions array is configured")
  void expectCompositeNodeWhenActionsDefined(Vertx vertx) throws Throwable {
    verify("tasks/compositeNode.conf", config -> {
      TaskOptions taskOptions = new TaskOptions(config);
      NodeOptions nodeOptions = taskOptions.getConfig();
      assertTrue(nodeOptions.isComposite());
      assertNull(nodeOptions.getAction());
      assertFalse(nodeOptions.getActions().isEmpty());
    }, vertx);
  }

  @Test
  @DisplayName("Expect custom Task builder with task containing 'a' action.")
  void expectCustomTaskBuilder(Vertx vertx) throws Throwable {
    verify("tasks/actionNodeLong.conf", config -> {
      TaskOptions options = new TaskOptions(config);
      assertEquals("custom", options.getFactory().getName());
      assertEquals(new JsonObject().put("anyKey", "anyValue"), options.getFactory().getConfig());
      assertEquals("simple", options.getConfig().getAction());
    }, vertx);
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