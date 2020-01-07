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

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.FragmentEventContextTaskAware;
import io.knotx.fragments.engine.FragmentsEngine;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.handler.consumer.FragmentEventsConsumer;
import io.knotx.fragments.handler.consumer.FragmentEventsConsumerProvider;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.api.context.RequestContext;
import io.knotx.server.api.context.RequestEvent;
import io.knotx.server.api.handler.DefaultRequestContextEngine;
import io.knotx.server.api.handler.RequestContextEngine;
import io.knotx.server.api.handler.RequestEventHandlerResult;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FragmentsHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FragmentsHandler.class);

  private final RequestContextEngine requestContextEngine;

  private final FragmentsEngine engine;
  private final TaskProvider taskProvider;
  private final FragmentEventsConsumerProvider fragmentEventsConsumerProvider;

  FragmentsHandler(Vertx vertx, JsonObject options) {
    FragmentsHandlerOptions handlerOptions = new FragmentsHandlerOptions(options);
    taskProvider = new TaskProvider(handlerOptions.getTaskFactories(), vertx);
    engine = new FragmentsEngine(vertx);
    requestContextEngine = new DefaultRequestContextEngine(getClass().getSimpleName());
    fragmentEventsConsumerProvider = new FragmentEventsConsumerProvider();
  }

  @Override
  public void handle(RoutingContext routingContext) {
    final RequestContext requestContext = routingContext.get(RequestContext.KEY);
    final List<Fragment> fragments = routingContext.get("fragments");
    final ClientRequest clientRequest = requestContext.getRequestEvent().getClientRequest();

    Single<List<FragmentEvent>> doHandle = doHandle(fragments, clientRequest);
    doHandle
        .doOnSuccess(events -> putFragments(routingContext, events))
        .doOnSuccess(events -> addAdditionalDataToFragments(clientRequest, events))
        .map(events -> toHandlerResult(events, requestContext))
        .subscribe(
            result -> requestContextEngine
                .processAndSaveResult(result, routingContext, requestContext),
            error -> requestContextEngine.handleFatal(routingContext, requestContext, error)
        );
  }

  protected Single<List<FragmentEvent>> doHandle(List<Fragment> fragments,
      ClientRequest clientRequest) {
    return Single.just(fragments)
        .map(f -> toEvents(f, clientRequest))
        .flatMap(engine::execute);
  }

  private void addAdditionalDataToFragments(ClientRequest clientRequest, List<FragmentEvent> fragmentEvents){
    List<FragmentEventsConsumer> consumers = fragmentEventsConsumerProvider.provide(getConsumers(clientRequest));
    consumers.forEach(consumer -> consumer.accept(fragmentEvents));
  }

  private List<String> getConsumers(ClientRequest clientRequest) {
    return clientRequest.getParams().getAll("consumers");
  }

  private void putFragments(RoutingContext routingContext, List<FragmentEvent> events) {
    routingContext.put("fragments", retrieveFragments(events, fragmentEvent -> true));
  }

  private RequestEventHandlerResult toHandlerResult(List<FragmentEvent> events,
      RequestContext requestContext) {

    List<Fragment> failedFragments = retrieveFragments(events,
        e -> e.getStatus() == Status.FAILURE);

    if (!failedFragments.isEmpty()) {
      return RequestEventHandlerResult.fail(buildErrorMessage(failedFragments));
    }

    return RequestEventHandlerResult.success(copyRequestEvent(requestContext.getRequestEvent()));
  }

  private String buildErrorMessage(List<Fragment> fragments) {
    return String.format("Following fragments processing failed: %s", fragmentIds(fragments));
  }

  private String fragmentIds(List<Fragment> fragments) {
    return fragments.stream()
        .map(Fragment::getId)
        .collect(Collectors.joining(", "));
  }

  private RequestEvent copyRequestEvent(RequestEvent requestEvent) {
    return new RequestEvent(requestEvent.getClientRequest(), requestEvent.getPayload());
  }

  private List<Fragment> retrieveFragments(List<FragmentEvent> events,
      Predicate<FragmentEvent> predicate) {
    return events.stream()
        .filter(predicate)
        .map(FragmentEvent::getFragment)
        .collect(Collectors.toList());
  }

  private List<FragmentEventContextTaskAware> toEvents(List<Fragment> fragments,
      ClientRequest clientRequest) {
    LOGGER.trace("Processing fragments [{}]", fragments);
    return fragments.stream()
        .map(
            fragment -> {
              FragmentEventContext fragmentEventContext = new FragmentEventContext(
                  new FragmentEvent(fragment), clientRequest);

              return taskProvider.newInstance(fragmentEventContext)
                  .map(task -> {
                    LOGGER.trace("Created task [{}] for fragment [{}]", task,
                        fragmentEventContext.getFragmentEvent().getFragment().getId());
                    return task;
                  })
                  .map(
                      task -> new FragmentEventContextTaskAware(task, fragmentEventContext))
                  .orElseGet(() -> new FragmentEventContextTaskAware(new Task("_NOT_DEFINED"),
                      fragmentEventContext));
            })
        .collect(
            Collectors.toList());
  }

}
