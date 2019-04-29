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

import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.api.exception.ActionFatalException;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Optional;

class TaskExecutionContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutionContext.class);

  private final String taskName;
  private final FragmentEventContext fragmentEventContext;
  private Node currentNode;

  TaskExecutionContext(String taskName, Node graphRoot,
      FragmentEventContext fragmentEventContext) {
    this.taskName = taskName;
    this.currentNode = graphRoot;
    this.fragmentEventContext = fragmentEventContext;
  }

  TaskExecutionContext(TaskExecutionContext context, Node currentNode) {
    FragmentEvent fragmentEvent = new FragmentEvent(
        context.getFragmentEventContext().getFragmentEvent().getFragment());
    this.fragmentEventContext = new FragmentEventContext(fragmentEvent,
        context.getFragmentEventContext().getClientRequest());
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
    if (isFatal(error)) {
      LOGGER.error("Processing failed with fatal error [{}].", fragmentEvent, error);
      throw (ActionFatalException) error;
    } else {
      LOGGER.warn("Knot processing failed [{}], trying to process with the 'error' transition.",
          fragmentEvent, error);
      fragmentEvent.setStatus(Status.FAILURE);
      if (isTimeout(error)) {
        fragmentEvent.log(EventLogEntry.timeout(taskName, currentNode.getId()));
      } else {
        fragmentEvent.log(EventLogEntry.error(taskName, currentNode.getId(), ERROR_TRANSITION));
      }
      return Single.just(new FragmentResult(fragmentEvent.getFragment(), ERROR_TRANSITION));
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
    FragmentEvent fe =fragmentEventContext.getFragmentEvent();
    final String nextTransition;
    if (fe.getStatus() == Status.FAILURE) {
      nextTransition = ERROR_TRANSITION;
    } else {
      nextTransition = SUCCESS_TRANSITION;
      if (fe.getStatus() == Status.SUCCESS) {
        handleSuccess(nextTransition);
      }
    }
    return new FragmentResult(fe.getFragment(), nextTransition);
  }

  boolean hasNext() {
    return currentNode != null;
  }

  void updateResult(FragmentResult fragmentResult) {
    fragmentEventContext.getFragmentEvent().setFragment(fragmentResult.getFragment());
    Optional<Node> nextNode = currentNode.next(fragmentResult.getTransition());
    if (nextNode.isPresent()) {
      currentNode = nextNode.get();
    } else {
      ifNotDefaultTransitionEndAsUnsupportedFailure(fragmentResult.getTransition());
      currentNode = null;
    }
  }

  void handleSuccess(String transition) {
    FragmentEvent fragmentEvent = fragmentEventContext.getFragmentEvent();
    fragmentEvent.setStatus(Status.SUCCESS);
    fragmentEvent
        .log(EventLogEntry.success(taskName, currentNode.getId(), transition));
  }

  private boolean isTimeout(Throwable error) {
    return error instanceof ReplyException
        && ((ReplyException) error).failureType() == ReplyFailure.TIMEOUT;
  }

  private boolean isFatal(Throwable error) {
    return error instanceof ActionFatalException;
  }

  private void ifNotDefaultTransitionEndAsUnsupportedFailure(String transition) {
    if (!SUCCESS_TRANSITION.equals(transition)) {
      FragmentEvent fragmentEvent = fragmentEventContext.getFragmentEvent();
      fragmentEvent.setStatus(Status.FAILURE);
      fragmentEvent.log(EventLogEntry.unsupported(taskName, currentNode.getId(), transition));
    }
  }
}
