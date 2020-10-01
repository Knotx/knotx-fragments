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
package io.knotx.fragments.task.engine;

import io.knotx.fragments.task.engine.TaskResult.Status;
import io.knotx.fragments.task.api.Node;
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
  public Single<List<TaskResult>> execute(List<FragmentContextTaskAware> fragments) {
    return Flowable.just(fragments)
        .concatMap(Flowable::fromIterable)
        .flatMapSingle(ctx -> ctx
            .getTask()
            .getRootNode()
            .map(rootNode -> startTaskEngine(ctx, rootNode))
            .orElseGet(() -> Single.just(new TaskResult(ctx.getTask().getName(), ctx.getFragmentContext().getFragment())))
        )
        .toList()
        .map(list -> incomingOrder(list, fragments))
        .map(this::traceEngineResults);
  }

  private Single<TaskResult> startTaskEngine(FragmentContextTaskAware context, Node rootNode) {
      return taskEngine.start(context.getTask().getName(), rootNode, context.getFragmentContext());
  }

  private List<TaskResult> incomingOrder(
      List<TaskResult> list, List<FragmentContextTaskAware> sourceEvents) {

    return sourceEvents.stream()
        .map(context -> context.getFragmentContext().getFragment().getId())
        .map(id -> getFragmentFromListById(id, list))
        .collect(Collectors.toList());
  }

  private TaskResult getFragmentFromListById(String id, List<TaskResult> events) {
    return events
            .stream()
            .filter(event -> id.equals(event.getFragment().getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Could not find fragment with id: " + id));
  }

  private List<TaskResult> traceEngineResults(List<TaskResult> results) {
    if (LOGGER.isTraceEnabled()) {
      List<TaskResult> processedEvents = results.stream()
          .filter(event -> Status.UNPROCESSED != event.getStatus())
          .collect(Collectors.toList());
      LOGGER.trace("Task Engine processed fragments: [{}]", processedEvents);
    }
    return results;
  }
}
