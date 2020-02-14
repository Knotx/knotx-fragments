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

import io.knotx.fragments.engine.FragmentEvent.Status;
import io.knotx.fragments.engine.api.node.Node;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fragments Engine processes fragment events in the request scope. Each fragment event contains a
 * fragment and an event metadata such as status, event log. All fragment events are processed
 * asynchronously according to the graph. The engine uses the Map-Reduce pattern where list of
 * events (fragments) is transformed to single items and processed independently. The inspiration
 * comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
public class FragmentsEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(FragmentsEngine.class);

  private final TaskEngine taskEngine;

  public FragmentsEngine(Vertx vertx) {
    this.taskEngine = new TaskEngine(vertx.getDelegate());
  }

  /**
   * Processes fragment events asynchronously.
   *
   * @param fragments list of fragment events with assigned {@code Task}
   * @return asynchronous response containing processed list of fragment events returned in the same
   * order as the original list
   */
  public Single<List<FragmentEventContextTaskAware>> execute(List<FragmentEventContextTaskAware> fragments) {
    return Flowable.just(fragments)
        .concatMap(Flowable::fromIterable)
        .flatMap(fragmentCtx -> fragmentCtx
            .getTask()
            .getRootNode()
            .map(rootNode -> startTaskEngine(fragmentCtx, rootNode))
            .orElseGet(() -> Single.just(fragmentCtx.getFragmentEventContext().getFragmentEvent()))
            .toFlowable()
            .map(event -> wrapIntoPreviousContext(event, fragmentCtx))
        )
        .toList()
        .map(list -> incomingOrder(list, fragments))
        .map(this::traceEngineResults);
  }

  private FragmentEventContextTaskAware wrapIntoPreviousContext(FragmentEvent event, FragmentEventContextTaskAware fragmentCtx) {
    return new FragmentEventContextTaskAware(
        fragmentCtx.getTask(),
        new FragmentEventContext(
            event,
            fragmentCtx.getFragmentEventContext().getClientRequest()
        )
    );
  }

  private Single<FragmentEvent> startTaskEngine(FragmentEventContextTaskAware fragment, Node rootNode) {
      return taskEngine.start(fragment.getTask().getName(), rootNode, fragment.getFragmentEventContext());
  }

  private List<FragmentEventContextTaskAware> incomingOrder(
      List<FragmentEventContextTaskAware> list, List<FragmentEventContextTaskAware> sourceEvents) {

    return sourceEvents.stream()
        .map(event -> event.getFragmentEventContext().getFragmentEvent().getFragment().getId())
        .map(id -> getFragmentFromListById(id, list))
        .collect(Collectors.toList());
  }

  private FragmentEventContextTaskAware getFragmentFromListById(String id, List<FragmentEventContextTaskAware> events) {
    return events
            .stream()
            .filter(event -> id.equals(event.getFragmentEventContext().getFragmentEvent().getFragment().getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Could not find fragment with id: " + id));
  }

  private List<FragmentEventContextTaskAware> traceEngineResults(List<FragmentEventContextTaskAware> results) {
    if (LOGGER.isTraceEnabled()) {
      List<FragmentEvent> processedEvents = results.stream()
          .map(FragmentEventContextTaskAware::getFragmentEventContext)
          .map(FragmentEventContext::getFragmentEvent)
          .filter(event -> Status.UNPROCESSED != event.getStatus())
          .collect(Collectors.toList());
      LOGGER.trace("Knot Engine processed fragments: [{}]", processedEvents);
    }
    return results;
  }
}
