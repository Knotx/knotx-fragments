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
import java.util.List;

class GraphEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(GraphEngine.class);

  private final Vertx vertx;

  GraphEngine(Vertx vertx) {
    this.vertx = vertx;
  }

  Single<FragmentEvent> start(FragmentEventContextTaskAware fragmentEventContextTaskAware) {
    Task task = fragmentEventContextTaskAware.getTask();
    TaskExecutionContext executionContext = new TaskExecutionContext(task.getName(),
        task.getRootNode(), fragmentEventContextTaskAware.getFragmentEventContext());

    return processNode(executionContext)
        .map(ctx -> ctx.getFragmentEventContext().getFragmentEvent());
  }

  private Single<TaskExecutionContext> processNode(TaskExecutionContext context) {
    traceEvent(context);
    if (context.getCurrentNodes().isEmpty()) {
      return Single.just(context);
    } else if (context.getCurrentNodes().size() == 1) {
      return singleOperationAction(context, context.getCurrentNodes().get(0));
    } else {
      return parallelOperationAction(context, context.getCurrentNodes());
    }
  }

  private Single<TaskExecutionContext> singleOperationAction(TaskExecutionContext context,
      Node graphNode) {
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
          context.setCurrentNodes(graphNode.next(fr.getTransition()));
          return processNode(context);
        });
  }

  private Single<TaskExecutionContext> parallelOperationAction(TaskExecutionContext context,
      List<Node> graphNodes) {
    return Observable.fromIterable(graphNodes)
        .flatMap(graphNode -> {
          return processNode( new TaskExecutionContext(context, graphNode) );
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
          Node currentNode = mergedExecutionContext.getCurrent();
          FragmentEvent fr = mergedExecutionContext.getFragmentEventContext().getFragmentEvent();
          // Fixme Update log
//            updateEvent(context, fr);
          updateFragment(context, fr.getFragment());

          final String transition;
          if (fr.getStatus() == Status.FAILURE) {
            transition = ERROR_TRANSITION;
          } else {
            transition = DEFAULT_TRANSITION;
          }
          return currentNode.next(transition).map(context::setCurrent)
              .map(this::processNode)
              .orElseGet(() -> endProcessing(context,
                  new FragmentResult(fr.getFragment(), transition)));
        });
  }

  private void updateEvent(TaskExecutionContext context, FragmentResult result) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    if (!result.getTransition().equals(ERROR_TRANSITION)) {
      fragmentEvent.setStatus(Status.SUCCESS);
      // Fixme
      SingleOperationNode node = (SingleOperationNode) context.getCurrent();
      fragmentEvent
          .log(EventLogEntry.success(node.getTask(), node.getAction(), result.getTransition()));
    }
  }

  private void updateFragment(TaskExecutionContext context, Fragment resultFragment) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    fragmentEvent.setFragment(resultFragment);
  }


//  private Single<TaskExecutionContext> processNode(TaskExecutionContext context) {
//    traceEvent(context);
//    if (context.getCurrent() instanceof SingleOperationNode) {
//      SingleOperationNode current = (SingleOperationNode) context.getCurrent();
//      return singleOperationAction(context, current);
//    } else {
//      ParallelOperationsNode current = (ParallelOperationsNode) context.getCurrent();
//      return parallelOperationAction(context, current);
//    }
//  }

//  private Single<TaskExecutionContext> parallelOperationAction(TaskExecutionContext context,
//      ParallelOperationsNode current) {
//    return Observable.fromIterable(current.getParallelNodes())
//        .flatMap(graphNode -> {
//          TaskExecutionContext newContext = new TaskExecutionContext(context, graphNode);
//          //fixme cast
//          if (graphNode instanceof SingleOperationNode) {
//            return singleOperationAction(newContext, (SingleOperationNode) graphNode)
//                .toObservable();
//          } else {
//            return parallelOperationAction(newContext, (ParallelOperationsNode) graphNode)
//                .toObservable();
//          }
//        })
//        .reduce(context, (fectx1, fectx2) -> {
//          final FragmentEvent fragmentEvent1 = fectx1.getFragmentEventContext()
//              .getFragmentEvent();
//          final FragmentEvent fragmentEvent2 = fectx2.getFragmentEventContext()
//              .getFragmentEvent();
//
//          //reduce fragment body and payload
//          final Fragment fragment = fragmentEvent1.getFragment();
//          final Fragment fragment2 = fragmentEvent2.getFragment();
//          fragment.mergeInPayload(fragment2.getPayload());
//          fragment.setBody(fragment2.getBody());
//
//          //reduce status and logs
//          if (Status.FAILURE != fragmentEvent1.getStatus()) {
//            fragmentEvent1.setStatus(fragmentEvent2.getStatus());
//          }
//          fragmentEvent1.appendLog(fragmentEvent2.getLog());
//
//          return fectx1;
//        })
//        .flatMap(mergedExecutionContext -> {
//          Node currentNode = mergedExecutionContext.getCurrent();
//          FragmentEvent fr = mergedExecutionContext.getFragmentEventContext().getFragmentEvent();
//          // Fixme Update log
////            updateEvent(context, fr);
//          updateFragment(context, fr.getFragment());
//
//          final String transition;
//          if (fr.getStatus() == Status.FAILURE) {
//            transition = ERROR_TRANSITION;
//          } else {
//            transition = DEFAULT_TRANSITION;
//          }
//          return currentNode.next(transition).map(context::setCurrent)
//              .map(this::processNode)
//              .orElseGet(() -> endProcessing(context,
//                  new FragmentResult(fr.getFragment(), transition)));
//        });
//  }
//
//  private Single<TaskExecutionContext> singleOperationAction(TaskExecutionContext context,
//      SingleOperationNode graphNode) {
//    return Single.just(graphNode)
//        .observeOn(RxHelper.blockingScheduler(vertx))
//        .flatMap(gn -> {
//          FragmentEventContext fragmentEventContext = context.getFragmentEventContext();
//          FragmentContext fc = new FragmentContext(
//              fragmentEventContext.getFragmentEvent().getFragment(),
//              fragmentEventContext
//                  .getClientRequest());
//          return gn.doOperation(fc);
//        })
//        .onErrorResumeNext(error -> handleError(context, error))
//        .flatMap(fr -> {
//          updateEvent(context, fr);
//          updateFragment(context, fr.getFragment());
//          return graphNode.next(fr.getTransition()).map(context::setCurrent)
//              .map(this::processNode).orElseGet(() -> endProcessing(context, fr));
//        });
//  }

  private Single<TaskExecutionContext> endProcessing(TaskExecutionContext context,
      FragmentResult result) {
    if (!DEFAULT_TRANSITION.equals(result.getTransition())) {
      FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
      fragmentEvent.setStatus(Status.FAILURE);

      //fixme - when parallel action fails, action should be different than the name of failing
      if (context.getCurrent() instanceof SingleOperationNode) {
        SingleOperationNode node = (SingleOperationNode) context.getCurrent();
        fragmentEvent
            .log(EventLogEntry
                .unsupported(node.getTask(), node.getAction(), result.getTransition()));
      } else {
        ParallelOperationsNode current = (ParallelOperationsNode) context.getCurrent();
        fragmentEvent
            .log(EventLogEntry
                .unsupported(context.getTask(), "parallel", result.getTransition()));
      }
    }
    context.end();
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
    //fixme
    SingleOperationNode node = (SingleOperationNode) context.getCurrent();
    if (isTimeout(error)) {
      fragmentEvent
          .log(EventLogEntry.timeout(node.getTask(), node.getAction()));
    } else {
      fragmentEvent
          .log(EventLogEntry.error(node.getTask(), node.getAction(), ERROR_TRANSITION));
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
          context.getCurrent());
    }
    return context;
  }

}
