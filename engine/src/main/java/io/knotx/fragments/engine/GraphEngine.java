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
import io.vertx.serviceproxy.ServiceException;
import java.util.Collections;
import java.util.stream.Collectors;

class GraphEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(GraphEngine.class);

  private final Vertx vertx;

  GraphEngine(Vertx vertx) {
    this.vertx = vertx;
  }

  Single<FragmentEvent> start(FragmentEventContext eventContext, GraphNode root) {
    FragmentExecutionContext executionContext = new FragmentExecutionContext()
        .setFragmentEventContext(eventContext).setGraphNodes(Collections.singletonList(root));

    return processNode(executionContext)
        .map(ctx -> ctx.getFragmentEventContext().getFragmentEvent());
  }

//  private Single<FragmentExecutionContext> processNode(FragmentExecutionContext context) {
//    return Single.just(context)
//        .map(this::traceEvent)
//        .observeOn(RxHelper.blockingScheduler(vertx))
////        .flatMap(this::doGraphNodeOperation)
//        .map(this::process);
////        .flatMap(ctx -> {
////          if (ctx.isLast()) {
////            return Single.just(ctx);
////          } else {
////            return processNode(ctx);
////          }
////        });
//  }

  private void updateEvent(FragmentExecutionContext context, FragmentResult result) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    fragmentEvent.setStatus(Status.SUCCESS);
//    GraphNode node = context.getGraphNodes();
//    fragmentEvent
//        .log(EventLogEntry.success(node.getTask(), node.getAction(), result.getTransition()));
  }

  private void updateFragment(FragmentExecutionContext context, FragmentResult result) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    fragmentEvent.setFragment(result.getFragment());
  }


  private Single<FragmentExecutionContext> processNode(FragmentExecutionContext context) {
    traceEvent(context);
    return Observable.fromIterable(context.getGraphNodes())
        .flatMap(graphNode -> Single.just(graphNode)
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
              updateFragment(context, fr);
              return graphNode.next(fr.getTransition()).map(context::setGraphNodes)
                  .map(this::processNode).orElseGet(() -> endProcessing(context, fr));
            }).toObservable()
        )
        .reduce(context, (fragmentExecutionContext, fragmentExecutionContext2) -> {
          final Fragment fragment = fragmentExecutionContext.getFragmentEventContext()
              .getFragmentEvent().getFragment();
          final Fragment fragment2 = fragmentExecutionContext2.getFragmentEventContext()
              .getFragmentEvent().getFragment();
          fragment.mergeInPayload(fragment2.getPayload());
          fragment.setBody(fragment2.getBody());
          return fragmentExecutionContext;
        });
  }

  private Single<FragmentExecutionContext> endProcessing(FragmentExecutionContext context,
      FragmentResult result) {
    if (!DEFAULT_TRANSITION.equals(result.getTransition())) {
      FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
      fragmentEvent.setStatus(Status.FAILURE);
//      GraphNode node = context.getGraphNodes();
//      fragmentEvent
//          .log(EventLogEntry
//              .unsupported(node.getTask(), node.getAction(), result.getTransition()));
    }
    context.end();
    return Single.just(context);
  }

  private SingleSource<? extends FragmentResult> handleError(
      FragmentExecutionContext context,
      Throwable error) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    if (isFatal(error)) {
      LOGGER.error("Processing failed with fatal error [{}].", fragmentEvent,
          error);
      throw new KnotProcessingFatalException(
          new Fragment(((ServiceException) error).getDebugInfo()));
    } else {
      LOGGER.warn("Knot processing failed [{}], trying to process with the 'error' transition.",
          fragmentEvent, error);
      fragmentEvent.setStatus(Status.FAILURE);
//      updateEventLog(error, context);
      return Single
          .just(new FragmentResult(fragmentEvent.getFragment(), ERROR_TRANSITION));
    }
  }

//  private void updateEventLog(Throwable error, FragmentExecutionContext context) {
//    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
//    GraphNode node = context.getGraphNodes();
//    if (isTimeout(error)) {
//      fragmentEvent
//          .log(EventLogEntry.timeout(node.getTask(), node.getAction()));
//    } else {
//      fragmentEvent
//          .log(EventLogEntry.error(node.getTask(), node.getAction(), ERROR_TRANSITION));
//    }
//  }

  private boolean isTimeout(Throwable error) {
    return
        error instanceof ReplyException
            && ((ReplyException) error).failureType() == ReplyFailure.TIMEOUT;
  }

  private boolean isFatal(Throwable error) {
    return error instanceof KnotProcessingFatalException;
  }

  private FragmentExecutionContext traceEvent(FragmentExecutionContext context) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Fragment event [{}] is processed via graph node [{}].",
          context.getFragmentEventContext().getFragmentEvent(),
          context.getGraphNodes().stream().map(GraphNode::getAction).collect(Collectors.toSet()));
    }
    return context;
  }

}
