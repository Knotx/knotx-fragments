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
package io.knotx.fragments.engine;

import io.knotx.fragments.engine.graph.ActionNode;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.RxHelper;

class TaskEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskEngine.class);

  private final Vertx vertx;

  TaskEngine(Vertx vertx) {
    this.vertx = vertx;
  }

  Single<FragmentEvent> start(String taskName, Node rootNode, FragmentEventContext fec) {
    TaskExecutionContext executionContext = new TaskExecutionContext(taskName, rootNode, fec);

    return processTask(executionContext)
        .map(ctx -> ctx.getFragmentEventContext().getFragmentEvent());
  }

  private Single<TaskExecutionContext> processTask(TaskExecutionContext context) {
    traceEvent(context);
    final Single<FragmentResult> result;
    if (context.getCurrentNode().isComposite()) {
      result = mapReduce(context);
    } else {
      result = execute(context);
    }
    return result.flatMap(fragmentResult -> {
      context.updateResult(fragmentResult);
      if (context.hasNext()) {
        return processTask(context);
      } else {
        return Single.just(context);
      }
    });
  }

  private Single<FragmentResult> execute(TaskExecutionContext context) {
    return Single.just((ActionNode) context.getCurrentNode())
        .observeOn(RxHelper.blockingScheduler(vertx))
        .flatMap(gn -> gn.doAction(context.fragmentContextInstance()))
        .doOnSuccess(fr -> context.handleSuccess(fr.getTransition()))
        .onErrorResumeNext(context::handleError);
  }

  private Single<FragmentResult> mapReduce(TaskExecutionContext context) {
    return Observable.fromIterable(((CompositeNode) context.getCurrentNode()).getNodes())
        .flatMap(
            graphNode -> processTask(new TaskExecutionContext(context, graphNode)).toObservable())
        .reduce(context, TaskExecutionContext::merge)
        .map(TaskExecutionContext::toFragmentResult);
  }

  private void traceEvent(TaskExecutionContext context) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Fragment event [{}] is processed via graph node [{}].",
          context.getFragmentEventContext().getFragmentEvent(),
          context.getCurrentNode());
    }
  }

}