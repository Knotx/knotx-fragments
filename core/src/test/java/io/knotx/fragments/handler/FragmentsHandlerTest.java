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

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
class FragmentsHandlerTest {

  @Test
  @DisplayName("Expect Status Code 200 when task completes on _success.")
  public void shouldSuccess(Vertx vertx, VertxTestContext testContext)
      throws IOException {
    //given
    int port = 9998;
    JsonObject config = from("tasks/fragment-handler/successAction.json");
    FragmentsHandler underTest = new FragmentsHandler(vertx, config);
    mockServer(vertx, underTest, fragment("success-task"), port);

    //when
    WebClient.create(vertx)
        .get(port, "localhost", "/")
        .send(testContext.succeeding(response -> testContext.verify(() -> {
          //then
          assertEquals(200, response.statusCode());
          testContext.completeNow();
        })));
  }

  @Test
  @DisplayName("Expect Status Code 500 when task completes on non exist transition.")
  public void shouldFail(Vertx vertx, VertxTestContext testContext)
      throws IOException {
    //given
    int port = 9999;
    JsonObject config = from("tasks/fragment-handler/failingAction.json");
    FragmentsHandler underTest = new FragmentsHandler(vertx, config);
    mockServer(vertx, underTest, fragment("failing-task"), port);

    //when
    WebClient.create(vertx)
        .get(port, "localhost", "/")
        .send(testContext.succeeding(response -> testContext.verify(() -> {

          //then
          assertEquals(500, response.statusCode());
          testContext.completeNow();
        })));
  }

  private void mockServer(Vertx vertx, FragmentsHandler fragmentsHandler, Fragment fragment,
      int port) {
    ClientRequest clientRequest = new ClientRequest();

    RequestEvent requestEvent = new RequestEvent(clientRequest, new JsonObject());

    RequestContext requestContext = new RequestContext(requestEvent);
    Router router = Router.router(vertx);
    router.route("/")
        .handler(prepareRoutingContext(requestContext, newArrayList(fragment)))
        .handler(fragmentsHandler)
        .handler(prepareResponse());

    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(router);
    httpServer.listen(port);
  }

  private Handler<RoutingContext> prepareRoutingContext(RequestContext requestContext, List<Fragment> fragments) {
    return context -> {
      context.put(RequestContext.KEY, requestContext);
      context.put("fragments", fragments);
      context.next();
    };
  }

  private Handler<RoutingContext> prepareResponse() {
    return context -> {
      List<Fragment> fragments = context.get("fragments");
      context.response()
          .end(fragments
              .get(0)
              .getBody());
    };
  }

  private Fragment fragment(String task) {
    return new Fragment("type",
        new JsonObject().put(FragmentsHandlerOptions.DEFAULT_TASK_KEY, task), "");
  }

  private JsonObject from(String fileName) throws IOException {
    return new JsonObject(FileReader.readText(fileName));
  }
}