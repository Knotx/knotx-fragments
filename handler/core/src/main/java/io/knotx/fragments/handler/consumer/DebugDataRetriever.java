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
