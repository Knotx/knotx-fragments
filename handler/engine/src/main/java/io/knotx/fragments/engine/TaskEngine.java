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

import io.knotx.fragments.engine.graph.NodeType;
import io.knotx.fragments.engine.graph.SingleNode;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.api.domain.FragmentResult;
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

    return context.hasNext()
            ? getResult(context).flatMap(fragmentResult -> {
                context.updateResult(fragmentResult);
                return processTask(context);
            })
            : Single.just(context);
  }

  private Single<TaskExecutionContext> processTask(TaskExecutionContext context, Node currentNode) {
    return processTask(new TaskExecutionContext(context, currentNode));
  }

  private Single<FragmentResult> getResult(TaskExecutionContext context) {
    return NodeType.COMPOSITE == context.getCurrentNode().getType()
            ? mapReduce(context)
            : execute(context);
  }

  private Single<FragmentResult> execute(TaskExecutionContext context) {
    return Single.just(context.getCurrentNode())
        .map(SingleNode.class::cast)
        .observeOn(RxHelper.blockingScheduler(vertx))
        .flatMap(gn -> gn.execute(context.fragmentContextInstance()))
        .doOnSuccess(context::handleSuccess)
        .onErrorResumeNext(context::handleError);
  }

  private Single<FragmentResult> mapReduce(TaskExecutionContext context) {
    CompositeNode node = (CompositeNode) context.getCurrentNode();
    return Observable.fromIterable(node.<Node>getNodes())
        .flatMap(graphNode -> processTask(context, graphNode).toObservable())
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