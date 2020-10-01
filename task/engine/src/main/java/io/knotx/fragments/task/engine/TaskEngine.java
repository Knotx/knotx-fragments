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
package io.knotx.fragments.task.engine;

import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.engine.node.NodeExecutor;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TaskEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskEngine.class);

  private final NodeExecutor executor;

  TaskEngine(Vertx vertx) {
    executor = new NodeExecutor(vertx, this);
  }

  public Single<TaskResult> start(String taskName, Node rootNode, FragmentContext fragmentContext) {
    return start(new TaskExecutionContext(taskName, fragmentContext, rootNode));
  }

  Single<TaskResult> start(TaskExecutionContext executionContext) {
    return processUntilFinished(executionContext)
        .map(TaskExecutionContext::getResult)
        .doOnError(executionContext::handleFatal);
  }

  Single<TaskExecutionContext> processUntilFinished(TaskExecutionContext context) {
    if (context.finished()) {
      return Single.just(context);
    } else {
      traceEvent(context);
      return executor.executeCurrentNode(context)
          .map(context::consumeResultAndShiftToNext)
          .flatMap(this::processUntilFinished);
    }
  }

  private void traceEvent(TaskExecutionContext context) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Fragment event [{}] is processed via graph node [{}].",
          context.getResult(),
          context.getCurrentNode());
    }
  }
}
