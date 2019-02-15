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

import io.knotx.engine.api.FragmentContext;
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
import java.util.Optional;
import java.util.stream.Collectors;

class KnotEngineImpl implements KnotEngine {

  private static final String DEFAULT_ERROR_TRANSITION = "error";
  private final Logger LOGGER = LoggerFactory.getLogger(KnotEngineImpl.class);

  private final Vertx vertx;
  private DeliveryOptions deliveryOptions;
  private final Map<String, KnotProxy> proxies;

  KnotEngineImpl(Vertx vertx, DeliveryOptions deliveryOptions) {
    this.vertx = vertx;
    this.deliveryOptions = deliveryOptions;
    this.proxies = new HashMap<>();
  }

  public Single<List<FragmentEvent>> execute(List<FragmentEvent> sourceEvents,
      ClientRequest clientRequest) {
    List<FragmentContext> sourceContextList = wrapContext(sourceEvents, clientRequest);
    return Flowable.fromIterable(sourceContextList)
        .map(this::execute)
        .flatMap(Single::toFlowable)
        .reduce(new ArrayList<FragmentContext>(), (list, item) -> {
          list.add(item);
          return list;
        })
        .map(list -> {
          list.sort(Comparator.comparingInt(FragmentContext::getOrder));
          return list;
        })
        .map(this::covert);
  }

  private List<FragmentContext> wrapContext(List<FragmentEvent> sourceEvent,
      ClientRequest clientRequest) {
    List<FragmentContext> result = new ArrayList<>(sourceEvent.size());
    for (int i = 0; i < sourceEvent.size(); i++) {
      result.add(new FragmentContext(sourceEvent.get(i), clientRequest, i + 1));
    }
    return result;
  }

  private Single<FragmentContext> execute(FragmentContext sourceCtx) {
    return Single.just(sourceCtx)
        .flatMap(this::callKnotProxy)
        .map(this::nextKnotFlow)
        .flatMap(event -> {
          FragmentContext processedCtx = new FragmentContext(event,
              sourceCtx.getClientRequest(), sourceCtx.getOrder());
          if (hasNextKnotFlow(event)) {
            return Single.just(processedCtx);
          } else {
            return execute(processedCtx);
          }
        });
  }

  private FragmentEvent nextKnotFlow(FragmentEventResult ctx) {
    Optional<KnotFlow> knotFlow = ctx.getFragmentEvent().getFlow();
    return knotFlow
        .map(flow -> {
          KnotFlow nextFlow = flow.get(ctx.getTransition());
          return new FragmentEvent(ctx.getFragmentEvent()).setFlow(nextFlow);
        })
        .orElse(ctx.getFragmentEvent());
  }

  private SingleSource<? extends FragmentEventResult> callKnotProxy(FragmentContext context) {
    Optional<KnotFlow> knotFlow = context.getFragmentEvent().getFlow();
    return knotFlow
        .map(flow -> proxies
            .computeIfAbsent(flow.getAddress(),
                adr -> KnotProxy
                    .createProxyWithOptions(vertx, adr, deliveryOptions))
            .rxProcess(context)
            .onErrorResumeNext(error -> handleProxyError(context, error))
        )
        .orElse(Single.just(new FragmentEventResult(context.getFragmentEvent(), null)));
  }

  private SingleSource<? extends FragmentEventResult> handleProxyError(FragmentContext context,
      Throwable error) {
    if (error instanceof ServiceException) {
      ServiceException serviceException = (ServiceException) error;
      if (serviceException.failureCode()
          == KnotProcessingFatalException.FAILURE_CODE) {
        throw new KnotProcessingFatalException(
            new Fragment(serviceException.getDebugInfo()));
      } else {
        // TODO refactor
        return Single
            .just(new FragmentEventResult(context.getFragmentEvent(), DEFAULT_ERROR_TRANSITION));
      }
    } else {
      return Single
          .just(new FragmentEventResult(context.getFragmentEvent(), DEFAULT_ERROR_TRANSITION));
    }
  }

  private boolean hasNextKnotFlow(FragmentEvent event) {
    return !event.getFlow().isPresent();
  }

  private List<FragmentEvent> covert(List<FragmentContext> fragmentContexts) {
    return fragmentContexts.stream().map(FragmentContext::getFragmentEvent)
        .collect(Collectors.toList());
  }
}