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

import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.composite.CompositeNode;
import io.knotx.fragments.task.engine.TaskEngine;
import io.knotx.fragments.task.engine.TaskResult;
import io.reactivex.Single;

class CompositeNodeExecutor {

  final TaskEngine taskEngine;

  public CompositeNodeExecutor(TaskEngine taskEngine) {
    this.taskEngine = taskEngine;
  }

  Single<NodeResult> mapReduce(NodeExecutionContext context) {
    return Single.just(context.getNode())
        .map(CompositeNode.class::cast)
        .flattenAsObservable(CompositeNode::getNodes)
        .flatMapSingle(subRoot -> startTaskEngine(context, subRoot))
        .reduce(TaskResult::merge)
        .map(context::onCompositeNodeFinish)
        .switchIfEmpty(context.compositeNodeEmptyHandler());
  }

  private Single<TaskResult> startTaskEngine(NodeExecutionContext context, Node subRoot) {
    return taskEngine.start(context.getTaskName(), subRoot, context.getFragmentContextCopy());
  }

}
