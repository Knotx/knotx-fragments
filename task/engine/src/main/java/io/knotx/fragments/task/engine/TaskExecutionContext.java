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

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.api.NodeFatalException;
import io.knotx.fragments.task.engine.FragmentEvent.Status;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

class TaskExecutionContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutionContext.class);

  private final String taskName;
  private final FragmentEventContext fragmentEventContext;
  private Node currentNode;

  TaskExecutionContext(String taskName, Node graphRoot, FragmentEventContext fragmentEventContext) {
    this.taskName = taskName;
    this.currentNode = graphRoot;
    this.fragmentEventContext = fragmentEventContext;
  }

  TaskExecutionContext(TaskExecutionContext context, Node currentNode) {
    Fragment fragment = context.getFragmentEventContext().getFragmentEvent().getFragment();
    FragmentEvent fragmentEvent = new FragmentEvent(fragment);
    ClientRequest clientRequest = context.getFragmentEventContext().getClientRequest();

    this.fragmentEventContext = new FragmentEventContext(fragmentEvent, clientRequest);
    this.currentNode = currentNode;
    this.taskName = context.taskName;
  }

  FragmentEventContext getFragmentEventContext() {
    return fragmentEventContext;
  }

  FragmentContext fragmentContextInstance() {
    return new FragmentContext(
        fragmentEventContext.getFragmentEvent().getFragment(),
        fragmentEventContext.getClientRequest());
  }

  Node getCurrentNode() {
    return currentNode;
  }

  SingleSource<? extends FragmentResult> handleError(Throwable error) {
    FragmentEvent fragmentEvent = fragmentEventContext.getFragmentEvent();
    fragmentEvent.setStatus(Status.FAILURE);
    fragmentEvent.log(EventLogEntry.exception(taskName, currentNode.getId(), ERROR_TRANSITION, error));
    if (isFatal(error)) {
      LOGGER
          .error("Processing failed with fatal error [{}].",
              fragmentEventContext.getFragmentEvent(),
              error);
      throw new TaskFatalException(fragmentEventContext);
    } else {
      LOGGER.warn("Knot processing failed [{}], trying to process with the 'error' transition.",
          fragmentEvent, error);
      return Single.just(
          new FragmentResult(fragmentEvent.getFragment(), ERROR_TRANSITION, new JsonObject()));
    }
  }

  TaskExecutionContext merge(TaskExecutionContext other) {
    final FragmentEvent fragmentEvent1 = getFragmentEventContext().getFragmentEvent();
    final FragmentEvent fragmentEvent2 = other.getFragmentEventContext().getFragmentEvent();

    //reduce fragment body and payload
    final Fragment fragment = fragmentEvent1.getFragment();
    final Fragment fragment2 = fragmentEvent2.getFragment();
    fragment.mergeInPayload(fragment2.getPayload());
    fragment.setBody(fragment2.getBody());

    //reduce status and logs
    if (Status.FAILURE != fragmentEvent1.getStatus()) {
      fragmentEvent1.setStatus(fragmentEvent2.getStatus());
    }
    fragmentEvent1.appendLog(fragmentEvent2.getLog());

    return this;
  }

  FragmentResult toFragmentResult() {
    FragmentEvent fragmentEvent = fragmentEventContext.getFragmentEvent();
    Status status = fragmentEvent.getStatus();
    String nextTransition = status.getDefaultTransition().orElse(null);
    FragmentResult result = new FragmentResult(fragmentEvent.getFragment(), nextTransition);
    if (status == Status.SUCCESS) {
      handleSuccess(result);
    } else {
      fragmentEvent
          .log(EventLogEntry.error(taskName, currentNode.getId(), result));
    }
    return result;
  }

  boolean hasNext() {
    return currentNode != null;
  }

  void updateResult(FragmentResult fragmentResult) {
    fragmentEventContext.getFragmentEvent().setFragment(fragmentResult.getFragment());

    currentNode = currentNode
        .next(fragmentResult.getTransition())
        .orElseGet(() -> {
          ifNotDefaultTransitionEndAsUnsupportedFailure(fragmentResult.getTransition());
          return null;
        });
  }

  void handleStarted() {
    FragmentEvent fragmentEvent = fragmentEventContext.getFragmentEvent();
    fragmentEvent.log(EventLogEntry.started(taskName, currentNode.getId()));
  }

  void handleSuccess(FragmentResult fragmentResult) {
    FragmentEvent fragmentEvent = fragmentEventContext.getFragmentEvent();
    fragmentEvent.setStatus(Status.SUCCESS);
    if (ERROR_TRANSITION.equals(fragmentResult.getTransition())) {
      fragmentEvent.log(EventLogEntry.error(taskName, currentNode.getId(), fragmentResult));
    } else {
      fragmentEvent.log(EventLogEntry.success(taskName, currentNode.getId(), fragmentResult));
    }
  }

  private boolean isFatal(Throwable error) {
    return error instanceof NodeFatalException;
  }

  private void ifNotDefaultTransitionEndAsUnsupportedFailure(String transition) {
    FragmentEvent fragmentEvent = fragmentEventContext.getFragmentEvent();
    if (!SUCCESS_TRANSITION.equals(transition)) {
      fragmentEvent.setStatus(Status.FAILURE);
      fragmentEvent.log(EventLogEntry.unsupported(taskName, currentNode.getId(), transition));
    }
  }
}
