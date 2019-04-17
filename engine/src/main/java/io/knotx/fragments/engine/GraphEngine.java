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

import static io.knotx.fragments.handler.api.fragment.FragmentResult.DEFAULT_TRANSITION;
import static io.knotx.fragments.handler.api.fragment.FragmentResult.ERROR_TRANSITION;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.engine.graph.ParallelOperationsNode;
import io.knotx.fragments.engine.graph.SingleOperationNode;
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
import java.util.Optional;

class GraphEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(GraphEngine.class);

  private final Vertx vertx;

  GraphEngine(Vertx vertx) {
    this.vertx = vertx;
  }

  Single<FragmentEvent> start(FragmentEventContextTaskAware fragmentContext) {
    Task task = fragmentContext.getTask();
    TaskExecutionContext executionContext = new TaskExecutionContext(task.getName(), task
        .getRootNode().get(), fragmentContext.getFragmentEventContext());

    return processNode(executionContext)
        .map(ctx -> ctx.getFragmentEventContext().getFragmentEvent());
  }

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


  private Single<TaskExecutionContext> processNode(TaskExecutionContext context) {
    traceEvent(context);
    if (context.getCurrentNode() instanceof SingleOperationNode) {
      SingleOperationNode current = (SingleOperationNode) context.getCurrentNode();
      return singleOperationAction(context, current);
    } else {
      ParallelOperationsNode current = (ParallelOperationsNode) context.getCurrentNode();
      return parallelOperationAction(context, current);
    }
  }

  private Single<TaskExecutionContext> parallelOperationAction(TaskExecutionContext context,
      ParallelOperationsNode current) {
    return Observable.fromIterable(current.getParallelNodes())
        .flatMap(graphNode -> {
          TaskExecutionContext newContext = new TaskExecutionContext(context, graphNode);
          //fixme cast
          if (graphNode instanceof SingleOperationNode) {
            return singleOperationAction(newContext, (SingleOperationNode) graphNode)
                .toObservable();
          } else {
            return parallelOperationAction(newContext, (ParallelOperationsNode) graphNode)
                .toObservable();
          }
        })
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
            nextTransition = DEFAULT_TRANSITION;
          }
          return currentNode.next(nextTransition).map(context::setCurrentNode)
              .map(this::processNode).orElseGet(() -> endProcessing(context, nextTransition));
        });
  }

  private Single<TaskExecutionContext> singleOperationAction(TaskExecutionContext context,
      SingleOperationNode graphNode) {
    return Single.just(graphNode)
        .observeOn(RxHelper.blockingScheduler(vertx))
        .flatMap(gn -> {
          FragmentEventContext fragmentEventContext = context.getFragmentEventContext();
          FragmentContext fc = new FragmentContext(
              fragmentEventContext.getFragmentEvent().getFragment(),
              fragmentEventContext
                  .getClientRequest());
          return gn.doOperation(fc);
        })
        .onErrorResumeNext(error -> handleError(context, error))
        .flatMap(fr -> {
          updateEvent(context, fr);
          updateFragment(context, fr.getFragment());
          return graphNode.next(fr.getTransition()).map(context::setCurrentNode)
              .map(this::processNode).orElseGet(() -> endProcessing(context, fr.getTransition()));
        });
  }

  private Single<TaskExecutionContext> endProcessing(TaskExecutionContext context,
      String transition) {
    if (!DEFAULT_TRANSITION.equals(transition)) {
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