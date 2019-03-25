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

import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.FragmentEvent.Status;
import io.knotx.engine.api.FragmentEventContextGraphAware;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Fragments Engine processes all fragment events from the request. Each fragment event contains
 * a fragment and an event metadata such as status, event log. All fragment events are processed
 * asynchronously according to the graph. The engine uses the Map-Reduce pattern where list of
 * events (fragments) is transformed to single items and processed independently. The inspiration
 * comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
public class FragmentsEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(FragmentsEngine.class);

  private final GraphEngine graphEngine;

  public FragmentsEngine(Vertx vertx) {
    this.graphEngine = new GraphEngine(vertx.getDelegate());
  }

  /**
   * Processes events asynchronously according to the {@link io.knotx.engine.api.GraphNode}.
   *
   * @param sourceEvents list of fragment events to process with graph context
   * @return asynchronous response containing processed list of fragment events returned in the same
   * order as the original list
   */
  public Single<List<FragmentEvent>> execute(List<FragmentEventContextGraphAware> sourceEvents) {

    return Flowable.just(sourceEvents)
        .concatMap(Flowable::fromIterable)
        .map(eventContext -> graphEngine
            .start(eventContext.getFragmentEventContext(), eventContext.getGraphNode()))
        .flatMap(Single::toFlowable)
        .reduce(new ArrayList<FragmentEvent>(), (list, item) -> {
          list.add(item);
          return list;
        })
        .map(list -> incomingOrder(list, sourceEvents))
        .map(this::traceEngineResults);
  }

  private List<FragmentEvent> incomingOrder(
      List<FragmentEvent> list, List<FragmentEventContextGraphAware> sourceEvents) {

    return sourceEvents.stream()
        .map(event -> event.getFragmentEventContext().getFragmentEvent().getFragment().getId())
        .map(
            id -> list.stream().filter(event -> id.equals(event.getFragment().getId())).findFirst()
                .orElseThrow(
                    () -> new IllegalStateException("Could not find frament with id: " + id)))
        .collect(Collectors.toList());
  }

  private List<FragmentEvent> traceEngineResults(List<FragmentEvent> results) {
    if (LOGGER.isTraceEnabled()) {
      List<FragmentEvent> processedEvents = results.stream()
          .filter(event -> Status.UNPROCESSED != event.getStatus())
          .collect(Collectors.toList());
      LOGGER.trace("Knot Engine processed fragments: [{}]", processedEvents);
    }
    return results;
  }
}