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
package io.knotx.fragments.utils;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;

public class HoconLoader {

  public static void verify(String fileName, Consumer<JsonObject> assertions,
      io.vertx.reactivex.core.Vertx vertx) throws Throwable {
    verify(fileName, assertions, vertx.getDelegate());
  }

  public static void verify(String fileName, Consumer<JsonObject> assertions, Vertx vertx)
      throws Throwable {
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

  public static void verifyAsync(String fileName, Consumer<JsonObject> execution,
      VertxTestContext testContext, io.vertx.reactivex.core.Vertx vertx)
      throws Throwable {
    verifyAsync(fileName, execution, testContext, vertx.getDelegate());
  }

  public static void verifyAsync(String fileName, Consumer<JsonObject> execution,
      VertxTestContext testContext, Vertx vertx)
      throws Throwable {
    Handler<AsyncResult<JsonObject>> configHandler = testContext
        .succeeding(config -> testContext.verify(() -> {
          execution.accept(config);
        }));
    fromHOCON(fileName, vertx, configHandler);

    Assertions.assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
    if (testContext.failed()) {
      throw testContext.causeOfFailure();
    }
  }

  private static void fromHOCON(String fileName, Vertx vertx,
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
