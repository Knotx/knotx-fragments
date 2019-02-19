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
package io.knotx.engine.core.impl;

import io.knotx.engine.api.FragmentEventContext;
import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.FragmentEventResult;
import io.knotx.engine.api.KnotFlow;
import io.knotx.engine.api.KnotProcessingFatalException;
import io.knotx.engine.core.KnotEngine;
import io.knotx.engine.reactivex.api.KnotProxy;
import io.knotx.fragment.Fragment;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.serviceproxy.ServiceException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class KnotEngineImpl implements KnotEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(KnotEngineImpl.class);

  private static final String DEFAULT_ERROR_TRANSITION = "error";
  private static final String NO_TRANSITION = null;

  private final Vertx vertx;
  private DeliveryOptions deliveryOptions;
  private final Map<String, KnotProxy> proxies;

  // TODO deliveryOptions should be specified per particular Knot per address
  // TODO then we can implement circuit breaker mechanism
  KnotEngineImpl(Vertx vertx, DeliveryOptions deliveryOptions) {
    this.vertx = vertx;
    this.deliveryOptions = deliveryOptions;
    this.proxies = new HashMap<>();
  }

  public Single<List<FragmentEvent>> execute(List<FragmentEvent> sourceEvents,
      ClientRequest clientRequest) {
    return Flowable.just(sourceEvents)
        .map(e -> wrapContext(e, clientRequest))
        .concatMap(Flowable::fromIterable)
        .map(this::execute)
        .flatMap(Single::toFlowable)
        .reduce(new ArrayList<FragmentEventContext>(), (list, item) -> {
          list.add(item);
          return list;
        })
        .map(this::sortAccordingToIncomigOrder)
        .map(this::covert);
  }

  private List<FragmentEventContext> wrapContext(List<FragmentEvent> sourceEvent,
      ClientRequest clientRequest) {
    List<FragmentEventContext> result = new ArrayList<>(sourceEvent.size());
    for (int i = 0; i < sourceEvent.size(); i++) {
      result.add(new FragmentEventContext(sourceEvent.get(i), clientRequest, i + 1));
    }
    return result;
  }

  private Single<FragmentEventContext> execute(FragmentEventContext sourceCtx) {
    return Single.just(sourceCtx)
        .map(this::traceMessage)
        .flatMap(this::callKnotProxy)
        .map(this::nextKnotFlow)
        .flatMap(event -> {
          FragmentEventContext processedCtx = new FragmentEventContext(event,
              sourceCtx.getClientRequest(), sourceCtx.getOrder());
          if (hasNextKnotFlow(event)) {
            return execute(processedCtx);
          } else {
            return Single.just(processedCtx);
          }
        });
  }

  private SingleSource<? extends FragmentEventResult> callKnotProxy(FragmentEventContext context) {
    return context.getFragmentEvent().getFlow()
        .map(flow -> proxies
            .computeIfAbsent(flow.getAddress(),
                adr -> KnotProxy
                    .createProxyWithOptions(vertx, adr, deliveryOptions))
            .rxProcess(context)
            .onErrorResumeNext(error -> handleProxyError(context, error))
        )
        .orElse(Single.just(new FragmentEventResult(context.getFragmentEvent(), NO_TRANSITION)));
  }

  private FragmentEvent nextKnotFlow(FragmentEventResult ctx) {
    return ctx.getFragmentEvent().getFlow()
        .map(currentFlow -> {
          KnotFlow nextFlow = currentFlow.get(ctx.getTransition());
          return new FragmentEvent(ctx.getFragmentEvent()).setFlow(nextFlow);
        })
        .orElse(ctx.getFragmentEvent());
  }

  private boolean hasNextKnotFlow(FragmentEvent event) {
    return event.getFlow().isPresent();
  }

  private SingleSource<? extends FragmentEventResult> handleProxyError(FragmentEventContext context,
      Throwable error) {
    if (isFatal(error)) {
      throw new KnotProcessingFatalException(
          new Fragment(((ServiceException) error).getDebugInfo()));
    } else {
      return Single
          .just(new FragmentEventResult(context.getFragmentEvent(), DEFAULT_ERROR_TRANSITION));
    }
  }

  private boolean isFatal(Throwable error) {
    boolean isServiceException = error instanceof ServiceException;
    if (isServiceException) {
      ServiceException serviceException = (ServiceException) error;
      return serviceException.failureCode() == KnotProcessingFatalException.FAILURE_CODE;
    }
    return false;
  }

  private ArrayList<FragmentEventContext> sortAccordingToIncomigOrder(ArrayList<FragmentEventContext> list) {
    list.sort(Comparator.comparingInt(FragmentEventContext::getOrder));
    return list;
  }

  private List<FragmentEvent> covert(List<FragmentEventContext> fragmentContexts) {
    return fragmentContexts.stream().map(FragmentEventContext::getFragmentEvent)
        .collect(Collectors.toList());
  }

  private FragmentEventContext traceMessage(FragmentEventContext fragmentContext) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("The next Knot is set to process the fragment [{}].",
          fragmentContext.getFragmentEvent());
    }
    return fragmentContext;
  }
}