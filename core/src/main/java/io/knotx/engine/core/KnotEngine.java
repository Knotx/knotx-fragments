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
import io.knotx.server.api.context.ClientRequest;
import io.reactivex.Single;
import java.util.List;

/**
 * The Knot Engine uses Knots to process all fragment events. Each fragment event contains a
 * fragment and an event metadata such as status, event log and flow. All fragment events are
 * processed asynchronously according to the flow (flow allows modeling of the event processing in
 * the form of a graph). The engine uses the Map-Reduce pattern where list of events (fragments) is
 * transformed to single items and processed independently. The inspiration comes from
 * https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 *
 * Every fragment event is processed through Knots (Knot's graph is defined as {@link
 * io.knotx.engine.api.KnotFlow}. Based on the Knot response (transition), the next Knot is chosen.
 *
 * More details: see io.knotx.engine.core.KnotEngineTest cases.
 */
public interface KnotEngine {

  /**
   * Processes all events asynchronously according to the {@link io.knotx.engine.api.KnotFlow}.
   *
   * @param events list of fragment events to process
   * @param clientRequest request context
   * @return asynchronous response containing processed list of fragment events returned in the same
   * order as the original list
   */
  Single<List<FragmentEvent>> execute(List<FragmentEvent> events,
      ClientRequest clientRequest);

}
