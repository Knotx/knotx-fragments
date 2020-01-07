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
package io.knotx.fragments.handler.consumer;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collector;

import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.JsonObject;
import javafx.util.Pair;

class DebugDataRetriever {

  static JsonObject retrieveDebugData(List<FragmentEvent> fragmentEvents, Predicate<FragmentEvent> eventFilter){
    return fragmentEvents.stream()
        .filter(eventFilter)
        .map(DebugDataRetriever::fragmentEventDebugData)
        .collect(Collector.of(JsonObject::new, DebugDataRetriever::putDebugData, JsonObject::mergeIn));
  }
  private static  Pair<String, JsonObject> fragmentEventDebugData(FragmentEvent fragmentEvent){
    return new Pair<>(fragmentEvent.getFragment().getId(), singleFragmentEventDebugData(fragmentEvent));
  }

  private static JsonObject singleFragmentEventDebugData(FragmentEvent fragmentEvent){
    return new JsonObject().put("payload", fragmentEvent.getFragment().getPayload())
        .put("logs", fragmentEvent.getLogAsJson())
        .put("body", fragmentEvent.getFragment().getBody());
  }

  private static void putDebugData(JsonObject debugData, Pair<String, JsonObject> fragmentEventDebugData){
    debugData.put(fragmentEventDebugData.getKey(), fragmentEventDebugData.getValue());
  }
}
