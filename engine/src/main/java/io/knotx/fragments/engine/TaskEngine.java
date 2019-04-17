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

import static io.knotx.fragments.handler.api.fragment.FragmentResult.SUCCESS_TRANSITION;
import static io.knotx.fragments.handler.api.fragment.FragmentResult.ERROR_TRANSITION;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.graph.ActionNode;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.CompositeNode;
import io.knotx.fragments.handler.api.exception.KnotProcessingFatalException;
import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
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

    return executeTask(executionContext)
        .map(ctx -> ctx.getFragmentEventContext().getFragmentEvent());
  }

  private Single<TaskExecutionContext> executeTask(TaskExecutionContext context) {
    traceEvent(context);
    if (context.getCurrentNode() instanceof ActionNode) {
      return singleOperationAction(context, (ActionNode) context.getCurrentNode());
    } else if (context.getCurrentNode() instanceof CompositeNode) {
      return parallelOperationAction(context, (CompositeNode) context.getCurrentNode());
    } else {
      throw new KnotProcessingFatalException(
          context.getFragmentEventContext().getFragmentEvent().getFragment());
    }
  }

  // =================================================

  private void updateEvent(TaskExecutionContext context, FragmentResult result) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    if (!result.getTransition().equals(ERROR_TRANSITION)) {
      fragmentEvent.setStatus(Status.SUCCESS);
      fragmentEvent
          .log(EventLogEntry.success(context.getTaskName(), context.getCurrentNode().getId(),
              result.getTransition()));
    }
  }

  private void updateFragment(TaskExecutionContext context, Fragment resultFragment) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    fragmentEvent.setFragment(resultFragment);
  }

  private Single<TaskExecutionContext> parallelOperationAction(TaskExecutionContext context,
      CompositeNode current) {
    return Observable.fromIterable(current.getNodes())
        .flatMap(
            graphNode -> executeTask(new TaskExecutionContext(context, graphNode)).toObservable())
        .reduce(context, (fectx1, fectx2) -> {
          final FragmentEvent fragmentEvent1 = fectx1.getFragmentEventContext()
              .getFragmentEvent();
          final FragmentEvent fragmentEvent2 = fectx2.getFragmentEventContext()
              .getFragmentEvent();

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

          return fectx1;
        })
        .flatMap(mergedExecutionContext -> {
          Node currentNode = mergedExecutionContext.getCurrentNode();
          FragmentEvent fr = mergedExecutionContext.getFragmentEventContext().getFragmentEvent();
          // Fixme Update log
//          updateEvent(context, fr);
          updateFragment(context, fr.getFragment());

          final String nextTransition;
          if (fr.getStatus() == Status.FAILURE) {
            nextTransition = ERROR_TRANSITION;
          } else {
            nextTransition = SUCCESS_TRANSITION;
          }
          return currentNode.next(nextTransition).map(context::setCurrentNode)
              .map(this::executeTask).orElseGet(() -> endProcessing(context, nextTransition));
        });
  }

  private Single<TaskExecutionContext> singleOperationAction(TaskExecutionContext context,
      ActionNode graphNode) {
    return Single.just(graphNode)
        .observeOn(RxHelper.blockingScheduler(vertx))
        .flatMap(gn -> {
          FragmentEventContext fragmentEventContext = context.getFragmentEventContext();
          FragmentContext fc = new FragmentContext(
              fragmentEventContext.getFragmentEvent().getFragment(),
              fragmentEventContext
                  .getClientRequest());
          return gn.doAction(fc);
        })
        .onErrorResumeNext(error -> handleError(context, error))
        .flatMap(fr -> {
          updateEvent(context, fr);
          updateFragment(context, fr.getFragment());
          return graphNode.next(fr.getTransition()).map(context::setCurrentNode)
              .map(this::executeTask).orElseGet(() -> endProcessing(context, fr.getTransition()));
        });
  }

  private Single<TaskExecutionContext> endProcessing(TaskExecutionContext context,
      String transition) {
    if (!SUCCESS_TRANSITION.equals(transition)) {
      FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
      fragmentEvent.setStatus(Status.FAILURE);
      fragmentEvent
          .log(EventLogEntry
              .unsupported(context.getTaskName(), context.getCurrentNode().getId(), transition));
    }
    return Single.just(context);
  }

  private SingleSource<? extends FragmentResult> handleError(
      TaskExecutionContext context,
      Throwable error) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    if (isFatal(error)) {
      LOGGER.error("Processing failed with fatal error [{}].", fragmentEvent, error);
      throw (KnotProcessingFatalException) error;
    } else {
      LOGGER.warn("Knot processing failed [{}], trying to process with the 'error' transition.",
          fragmentEvent, error);
      fragmentEvent.setStatus(Status.FAILURE);
      updateEventLog(error, context);
      return Single
          .just(new FragmentResult(fragmentEvent.getFragment(), ERROR_TRANSITION));
    }
  }

  private void updateEventLog(Throwable error, TaskExecutionContext context) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    Node node = context.getCurrentNode();
    if (isTimeout(error)) {
      fragmentEvent
          .log(EventLogEntry.timeout(context.getTaskName(), node.getId()));
    } else {
      fragmentEvent
          .log(EventLogEntry.error(context.getTaskName(), node.getId(), ERROR_TRANSITION));
    }
  }

  private boolean isTimeout(Throwable error) {
    return
        error instanceof ReplyException
            && ((ReplyException) error).failureType() == ReplyFailure.TIMEOUT;
  }

  private boolean isFatal(Throwable error) {
    return error instanceof KnotProcessingFatalException;
  }

  private TaskExecutionContext traceEvent(TaskExecutionContext context) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Fragment event [{}] is processed via graph node [{}].",
          context.getFragmentEventContext().getFragmentEvent(),
          context.getCurrentNode());
    }
    return context;
  }

}