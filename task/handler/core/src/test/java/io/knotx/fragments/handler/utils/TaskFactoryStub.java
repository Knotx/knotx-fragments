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
package io.knotx.fragments.handler.utils;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.Task;
import io.knotx.fragments.task.api.single.SingleNode;
import io.knotx.fragments.task.factory.api.TaskFactory;
import io.knotx.fragments.task.factory.api.metadata.TaskMetadata;
import io.knotx.fragments.task.factory.api.metadata.TaskWithMetadata;
import io.knotx.server.api.context.ClientRequest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import java.util.Optional;

public class TaskFactoryStub implements TaskFactory {

  private boolean accept;
  private String transition;

  @Override
  public String getName() {
    return "stub";
  }

  @Override
  public TaskFactory configure(JsonObject config, Vertx vertx) {
    JsonObject safeConfig = config == null ? new JsonObject() : config;
    this.accept = safeConfig.getBoolean("accept", Boolean.TRUE);
    this.transition = safeConfig.getString("transition", "_success");
    return this;
  }

  @Override
  public boolean accept(Fragment fragment, ClientRequest clientRequest) {
    return accept;
  }

  @Override
  public TaskWithMetadata newInstance(Fragment fragment, ClientRequest clientRequest) {
    Task task = new Task("taskName", new SingleNode() {
      @Override
      public void apply(FragmentContext fragmentContext,
          Handler<AsyncResult<FragmentResult>> resultHandler) {
        Fragment fragment = fragmentContext.getFragment();
        final Future<FragmentResult> future;
        fragment.setBody(transition);
        future = Future
            .succeededFuture(new FragmentResult(fragment, transition));
        future.setHandler(resultHandler);
      }

      @Override
      public String getId() {
        return "id";
      }

      @Override
      public Optional<Node> next(String transition) {
        return Optional.empty();
      }
    });
    return new TaskWithMetadata(task, TaskMetadata.notDefined());
  }
}
