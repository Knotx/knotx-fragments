package io.knotx.fragments.handler.debug;

import static java.util.stream.Collectors.joining;

import java.util.List;

import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NotSupportedDebugModeStrategy implements FragmentsDebugModeStrategy {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(NotSupportedDebugModeStrategy.class);

  @Override
  public void updateBodyWithDebugData(JsonObject debugData, List<FragmentEvent> fragmentEvents) {
    LOGGER.warn(
        "Debug mode is supported only for snippet amd JsonObject Fragment type. Current context contains following types: {}",
        getFragmentsType(fragmentEvents));
  }

  private String getFragmentsType(List<FragmentEvent> fragmentEvents) {
    return fragmentEvents.stream()
        .map(event -> event.getFragment().getType())
        .collect(joining(", "));
  }
}
