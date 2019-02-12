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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.engine.core;

import io.knotx.engine.api.FragmentContext;
import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.FragmentEventResult;
import io.knotx.engine.api.KnotFlow;
import io.knotx.engine.api.KnotProcessingFatalException;
import io.knotx.engine.reactivex.api.KnotProxy;
import io.knotx.fragment.Fragment;
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.serviceproxy.ServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KnotEngine {

  private final Logger LOGGER = LoggerFactory.getLogger(KnotEngine.class);

  private final Vertx vertx;
  private DeliveryOptions deliveryOptions;
  private KnotFlowProvider knotFlowProvider;
  private final Map<String, KnotProxy> proxies;

  public KnotEngine(Vertx vertx, KnotEngineHandlerOptions options) {
    this.vertx = vertx;
    this.deliveryOptions = options.getDeliveryOptions();
    this.knotFlowProvider = new KnotFlowProvider(options.getFlows());
    this.proxies = new HashMap<>();
  }

  public Single<List<FragmentEvent>> execute(List<FragmentEvent> events,
      ClientRequest clientRequest) {

    return Flowable.fromIterable(events)
        .map(event -> new FragmentContext(event, clientRequest))
        .map(this::execute)
        .flatMap(Single::toFlowable)
        .reduce(new ArrayList<>(), (list, item) -> {
          list.add(item);
          return list;
        });

  }

  private Single<FragmentEvent> execute(FragmentContext fragmentContext) {
    return Single.just(fragmentContext)
        .flatMap(
            context -> {
              return context.getFragmentEvent().getFlow().map(flow -> {
                return proxies
                    .computeIfAbsent(flow.getAddress(),
                        adr -> KnotProxy
                            .createProxyWithOptions(vertx, adr, deliveryOptions))
                    .rxProcess(context)
                    .onErrorResumeNext(error -> {
                      if (error instanceof ServiceException) {
                        ServiceException serviceException = (ServiceException) error;
                        if (serviceException.failureCode()
                            == KnotProcessingFatalException.FAILURE_CODE) {
                          throw new KnotProcessingFatalException(
                              new Fragment(serviceException.getDebugInfo()));
                        } else {
                          // TODO refactor
                          return Single
                              .just(new FragmentEventResult(context.getFragmentEvent(), "error"));
                        }
                      } else {
                        return Single
                            .just(new FragmentEventResult(context.getFragmentEvent(), "error"));
                      }
                    });
              }).orElse(Single.just(new FragmentEventResult(context.getFragmentEvent(), "")));
            })
        .map(ctx -> {
          return ctx.getFragmentEvent().getFlow().map(flow -> {
            KnotFlow nextFlow = flow.get(ctx.getTransition());
            return new FragmentEvent(ctx.getFragmentEvent()).setFlow(nextFlow);
          }).orElse(ctx.getFragmentEvent());

        })
        .flatMap(event -> {
          if (hasNextKnot(event)) {
            return Single.just(event);
          } else {
            return execute(new FragmentContext(event, fragmentContext.getClientRequest()));
          }
        });
  }

  private boolean hasNextKnot(FragmentEvent event) {
    return !event.getFlow().isPresent();
  }


}