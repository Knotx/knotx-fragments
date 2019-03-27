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
package io.knotx.engine.core;

import static io.knotx.engine.api.fragment.FragmentResult.DEFAULT_TRANSITION;
import static io.knotx.engine.api.fragment.FragmentResult.ERROR_TRANSITION;

import io.knotx.engine.core.FragmentEvent.Status;
import io.knotx.engine.api.exception.KnotProcessingFatalException;
import io.knotx.engine.api.fragment.FragmentContext;
import io.knotx.engine.api.fragment.FragmentResult;
import io.knotx.fragment.Fragment;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.RxHelper;
import io.vertx.serviceproxy.ServiceException;

class GraphEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(GraphEngine.class);

  private final Vertx vertx;

  GraphEngine(Vertx vertx) {
    this.vertx = vertx;
  }

  Single<FragmentEvent> start(FragmentEventContext eventContext, GraphNode root) {
    FragmentExecutionContext executionContext = new FragmentExecutionContext()
        .setFragmentEventContext(eventContext).setCurrentNode(root);

    return processNode(executionContext)
        .map(ctx -> ctx.getFragmentEventContext().getFragmentEvent());
  }

  private Single<FragmentExecutionContext> processNode(FragmentExecutionContext context) {
    return Single.just(context)
        .map(this::traceEvent)
        .observeOn(RxHelper.blockingScheduler(vertx))
        .flatMap(this::doGraphNodeOperation)
        .map(result -> setNextNode(result, context))
        .flatMap(ctx -> {
          if (ctx.isLast()) {
            return Single.just(ctx);
          } else {
            return processNode(ctx);
          }
        });
  }

  private SingleSource<? extends FragmentResult> doGraphNodeOperation(
      FragmentExecutionContext context) {
    FragmentContext fragmentContext = new FragmentContext(
        context.getFragmentEventContext().getFragmentEvent().getFragment(),
        context.getFragmentEventContext().getClientRequest());

    return Single.just(context)
        .map(FragmentExecutionContext::getCurrentNode)
        .flatMap(graphNode -> graphNode.doOperation(fragmentContext))
        .doOnSuccess(result -> updateFragment(context, result))
        .doOnSuccess(result -> updateEventStatus(context, result))
        .onErrorResumeNext(error -> handleError(context, error));
  }

  private void updateEventStatus(FragmentExecutionContext context, FragmentResult result) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    fragmentEvent.setStatus(Status.SUCCESS);
    fragmentEvent
        .log(EventLogEntry.success(context.getCurrentNode().getName(), result.getTransition()));
  }

  private void updateFragment(FragmentExecutionContext context, FragmentResult result) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    fragmentEvent.setFragment(result.getFragment());
  }


  private FragmentExecutionContext setNextNode(FragmentResult result,
      FragmentExecutionContext context) {
    return context.getCurrentNode().next(result.getTransition())
        .map(context::setCurrentNode)
        .orElseGet(() -> endProcessing(context, result));
  }

  private FragmentExecutionContext endProcessing(FragmentExecutionContext context,
      FragmentResult result) {
    if (!DEFAULT_TRANSITION.equals(result.getTransition())) {
      FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
      fragmentEvent.setStatus(Status.FAILURE);
      fragmentEvent
          .log(EventLogEntry
              .unsupported(context.getCurrentNode().getName(), result.getTransition()));
    }
    context.end();
    return context;
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
      updateEventLog(error, context);
      return Single
          .just(new FragmentResult(fragmentEvent.getFragment(), ERROR_TRANSITION));
    }
  }

  private void updateEventLog(Throwable error, FragmentExecutionContext context) {
    FragmentEvent fragmentEvent = context.getFragmentEventContext().getFragmentEvent();
    String graphNodeName = context.getCurrentNode().getName();
    if (isTimeout(error)) {
      fragmentEvent
          .log(EventLogEntry.timeout(graphNodeName));
    } else {
      fragmentEvent
          .log(EventLogEntry.error(graphNodeName, ERROR_TRANSITION));
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

  private FragmentExecutionContext traceEvent(FragmentExecutionContext context) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Fragment event [{}] is processed via graph node [{}].",
          context.getFragmentEventContext().getFragmentEvent(), context.getCurrentNode().getName());
    }
    return context;
  }

}
