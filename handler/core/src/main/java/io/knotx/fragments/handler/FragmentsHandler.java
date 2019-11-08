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

import static com.google.common.base.Predicates.alwaysTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.FragmentEventContext;
import io.knotx.fragments.engine.FragmentEventContextTaskAware;
import io.knotx.fragments.engine.FragmentsEngine;
import io.knotx.fragments.engine.Task;
import io.knotx.fragments.handler.options.FragmentsHandlerOptions;
import io.knotx.fragments.task.TaskProvider;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.api.context.RequestContext;
import io.knotx.server.api.context.RequestEvent;
import io.knotx.server.api.handler.DefaultRequestContextEngine;
import io.knotx.server.api.handler.RequestContextEngine;
import io.knotx.server.api.handler.RequestEventHandlerResult;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FragmentsHandler implements Handler<RoutingContext> {

  private final RequestContextEngine requestContextEngine;

  private final FragmentsEngine engine;
  private final TaskProvider taskProvider;

  FragmentsHandler(Vertx vertx, JsonObject config) {
    FragmentsHandlerOptions options = new FragmentsHandlerOptions(config);

    taskProvider = new TaskProvider(options.getTaskKey(), options.getTasks(), vertx);
    engine = new FragmentsEngine(vertx);
    requestContextEngine = new DefaultRequestContextEngine(getClass().getSimpleName());
  }

  @Override
  public void handle(RoutingContext routingContext) {
    RequestContext requestContext = routingContext.get(RequestContext.KEY);
    final List<Fragment> fragments = routingContext.get("fragments");

    ClientRequest clientRequest = requestContext.getRequestEvent().getClientRequest();

    Single.just(fragments)
        .map(f -> toEvents(f, clientRequest))
        .flatMap(engine::execute)
        .doOnSuccess(events -> putFragments(routingContext, events))
        .map(events -> toHandlerResult(events, requestContext))
        .subscribe(
            result -> requestContextEngine
                .processAndSaveResult(result, routingContext, requestContext),
            error -> requestContextEngine.handleFatal(routingContext, requestContext, error)
        );
  }

  private RoutingContext putFragments(RoutingContext routingContext, List<FragmentEvent> events) {
    return routingContext.put("fragments", retrieveFragments(events, alwaysTrue()));
  }

  private RequestEventHandlerResult toHandlerResult(List<FragmentEvent> events,
      RequestContext requestContext) {

    List<Fragment> failedFragments = retrieveFragments(events, hasStatus(Status.FAILURE));

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

  private Predicate<FragmentEvent> hasStatus(Status status) {
    return e -> e.getStatus() == status;
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
    return fragments.stream()
        .map(
            fragment -> {
              FragmentEventContext fragmentEventContext = new FragmentEventContext(
                  new FragmentEvent(fragment), clientRequest);
              return taskProvider.newInstance(fragmentEventContext)
                  .map(
                      task -> new FragmentEventContextTaskAware(task, fragmentEventContext))
                  .orElseGet(() -> new FragmentEventContextTaskAware(new Task("_NOT_DEFINED"),
                      fragmentEventContext));
            })
        .collect(
            Collectors.toList());
  }

}
