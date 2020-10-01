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

import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.NodeFatalException;
import io.knotx.fragments.task.engine.EventLog;
import io.knotx.fragments.task.engine.TaskFatalException;
import io.knotx.fragments.task.engine.TaskResult;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeExecutionContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(NodeExecutionContext.class);

  private final String taskName;
  private final NodeLogger logger;
  private final Node node;
  private final FragmentContext fragmentContext;

  public NodeExecutionContext(String taskName, Node node, EventLog eventLog,
      FragmentContext fragmentContext) {
    this.taskName = taskName;
    this.node = node;
    this.logger = new NodeLogger(eventLog);
    this.fragmentContext = fragmentContext;
  }

  public String getTaskName() {
    return taskName;
  }

  public Node getNode() {
    return node;
  }

  public FragmentContext getFragmentContextCopy() {
    return fragmentContext.copy();
  }

  public void onNodeStart() {
    logger.onNodeStart(node);
  }

  public NodeResult onSingleNodeFinish(FragmentResult fragmentResult) {
    logger.onResultDelivered(node, fragmentResult);
    return NodeResult.fromSingleResult(fragmentResult);
  }

  public NodeResult onCompositeNodeFinish(TaskResult jointEvent) {
    String compositeNodeTransition = jointEvent.getStatus().getDefaultTransition()
        .orElse(SUCCESS_TRANSITION);
    logger.onCompositeResult(node, jointEvent, compositeNodeTransition);
    return NodeResult.fromCompositeResult(jointEvent, compositeNodeTransition);
  }

  public NodeResult onNodeError(Throwable error) {
    // TODO: validate if logging should occur in all cases
    logger.onException(node, error);
    if (isFatal(error)) {
      // TODO: should log whole FragmentEvent, but does not
      //LOGGER
      //    .error("Processing failed with fatal error [{}].", this.fragmentEvent, error);
      throw (RuntimeException) error;
    }
    // TODO: should log whole FragmentEvent, but does not
    //LOGGER.warn("Node processing failed [{}], trying to process with the 'error' transition.",
    //   fragmentEvent, error);
    return NodeResult.error(fragmentContext.getFragment());
  }

  private boolean isFatal(Throwable error) {
    return error instanceof NodeFatalException // from node
        || error instanceof TaskFatalException // from task
        || (error instanceof CompositeException && isNestedFatal((CompositeException) error)
    );
  }

  private boolean isNestedFatal(CompositeException error) {
    return error.getExceptions().stream()
        .anyMatch(e -> e instanceof TaskFatalException || e instanceof NodeFatalException);
  }

  public NodeResult onCompositeNodeEmpty() {
    return NodeResult.fromEmptyComposite(fragmentContext.getFragment().copy());
  }

  // The code below is needed because there is no Maybe.switchIfEmpty or Maybe.toSingle
  // taking Supplier<NodeResult> as an argument.
  public Single<NodeResult> compositeNodeEmptyHandler() {
    return Single.defer(() -> Single.just(this.onCompositeNodeEmpty()));
  }
}

