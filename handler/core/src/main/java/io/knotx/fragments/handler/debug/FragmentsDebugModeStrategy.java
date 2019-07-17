package io.knotx.fragments.handler.debug;

import java.util.List;

import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.JsonObject;

public interface FragmentsDebugModeStrategy {
  void updateBodyWithDebugData(JsonObject debugData, List<FragmentEvent> fragmentEvents);
}
