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
package io.knotx.fragments.task.engine.node;

import io.knotx.fragments.task.api.NodeType;
import io.knotx.fragments.task.engine.TaskEngine;
import io.knotx.fragments.task.engine.TaskExecutionContext;
import io.reactivex.Single;
import io.vertx.core.Vertx;

public class NodeExecutor {

  private final SingleNodeExecutor singleExecutor;
  private final CompositeNodeExecutor compositeExecutor;

  public NodeExecutor(Vertx vertx, TaskEngine taskEngine) {
    this.singleExecutor = new SingleNodeExecutor(vertx);
    this.compositeExecutor = new CompositeNodeExecutor(taskEngine);
  }

  public Single<NodeResult> executeCurrentNode(TaskExecutionContext context) {
    return Single.just(context)
        .map(TaskExecutionContext::createNodeContext)
        .doOnSuccess(NodeExecutionContext::onNodeStart)
        .flatMap(this::doExecuteSafe);
  }

  private Single<NodeResult> doExecuteSafe(NodeExecutionContext nodeContext) {
    return doExecute(nodeContext)
        .onErrorReturn(nodeContext::onNodeError);
  }

  private Single<NodeResult> doExecute(NodeExecutionContext nodeContext) {
    return NodeType.COMPOSITE == nodeContext.getNode().getType()
        ? compositeExecutor.mapReduce(nodeContext)
        : singleExecutor.execute(nodeContext);
  }

}
