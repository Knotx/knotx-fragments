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
package io.knotx.fragments.handler;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.FragmentEventContextTaskAware;
import io.knotx.fragments.engine.FragmentsEngine;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.handler.action.ActionProvider;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.options.FragmentsHandlerOptions;
import io.knotx.fragments.task.TaskBuilder;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.api.context.RequestContext;
import io.knotx.server.api.context.RequestEvent;
import io.knotx.server.api.handler.DefaultRequestContextEngine;
import io.knotx.server.api.handler.RequestContextEngine;
import io.knotx.server.api.handler.RequestEventHandlerResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FragmentsHandler implements Handler<RoutingContext> {

  private final FragmentsEngine engine;
  private final RequestContextEngine requestContextEngine;
  private final TaskBuilder taskBuilder;
  private final FragmentsHandlerOptions options;
  private final HtmlFragmentsDebugModeDecorator debugModeDecorator;

  FragmentsHandler(Vertx vertx, JsonObject config) {
    options = new FragmentsHandlerOptions(config);

    ActionProvider proxyProvider = new ActionProvider(options.getActions(),
        supplyFactories(), vertx.getDelegate());
    taskBuilder = new TaskBuilder(options.getTaskKey(), options.getTasks(), proxyProvider);
    engine = new FragmentsEngine(vertx);
    requestContextEngine = new DefaultRequestContextEngine(getClass().getSimpleName());
    debugModeDecorator = new HtmlFragmentsDebugModeDecorator();
    if (options.isDebugMode()) {
      debugModeDecorator.init();
    }
  }

  @Override
  public void handle(RoutingContext routingContext) {
    RequestContext requestContext = routingContext.get(RequestContext.KEY);
    List<Fragment> fragments = requestContext.getRequestEvent().getFragments();
    ClientRequest clientRequest = requestContext.getRequestEvent().getClientRequest();

    boolean isDebugMode = isDebugModeOn(clientRequest);
    List<FragmentEventContextTaskAware> events = toEvents(fragments, clientRequest);
    if (isDebugMode) {
      events.forEach(e -> debugModeDecorator
          .appendFragmentBody(e.getFragmentEventContext().getFragmentEvent()));
    }

    engine.execute(events)
        .doOnSuccess(fragmentEvents -> {
          if (isDebugMode) {
            debugModeDecorator.addDebugAssetsAndData(fragmentEvents);
          }
        })
        .map(fragmentEvents -> toHandlerResult(fragmentEvents, requestContext))
        .subscribe(
            result -> requestContextEngine
                .processAndSaveResult(result, routingContext, requestContext),
            error -> requestContextEngine.handleFatal(routingContext, requestContext, error)
        );
  }

  private boolean isDebugModeOn(ClientRequest clientRequest) {
    return options.isDebugMode() && clientRequest.getParams().contains("debug");
  }

  private Supplier<Iterator<ActionFactory>> supplyFactories() {
    return () -> {
      ServiceLoader<ActionFactory> factories = ServiceLoader
          .load(ActionFactory.class);
      return factories.iterator();
    };
  }

  private RequestEventHandlerResult toHandlerResult(List<FragmentEvent> events,
      RequestContext requestContext) {
    RequestEvent requestEvent = updateRequestEvent(requestContext.getRequestEvent(), events);
    return RequestEventHandlerResult.success(requestEvent);
  }

  private RequestEvent updateRequestEvent(RequestEvent requestEvent, List<FragmentEvent> events) {
    // TODO implement error handling: now we process all fragments, even they are invalid
    List<Fragment> fragments = events.stream().map(FragmentEvent::getFragment)
        .collect(Collectors.toList());
    return new RequestEvent(requestEvent.getClientRequest(), fragments, requestEvent.getPayload());
  }

  private List<FragmentEventContextTaskAware> toEvents(List<Fragment> fragments,
      ClientRequest clientRequest) {
    return fragments.stream()
        .map(
            fragment -> {
              FragmentEventContext fragmentEventContext = new FragmentEventContext(
                  new FragmentEvent(fragment), clientRequest);
              return taskBuilder.build(fragment).map(
                  task -> new FragmentEventContextTaskAware(task, fragmentEventContext))
                  .orElseGet(() -> new FragmentEventContextTaskAware(new Task("_NOT_DEFINED"),
                      fragmentEventContext));
            })
        .collect(Collectors.toList());
  }
}
