package io.knotx.fragments.handler.consumer;

import java.util.List;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class JsonFragmentEventsConsumer implements FragmentEventsConsumer {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(JsonFragmentEventsConsumer.class);

  @Override
  public void accept(List<FragmentEvent> fragmentEvents) {
    JsonObject debugData = DebugDataRetriever.retrieveDebugData(fragmentEvents, this::isJson);
    fragmentEvents.stream()
        .filter(this::isJson)
        .findAny().ifPresent(f -> addDebugData(debugData,f.getFragment()));
  }

  private boolean isJson(FragmentEvent fragmentEvent){
    return "json".equals(fragmentEvent.getFragment().getType());
  }

  private void addDebugData(JsonObject debugData, Fragment fragment){
    try {
      JsonObject body = new JsonObject(fragment.getBody());
      body.put("debug", debugData);
      fragment.setBody(body.encode());
    }catch (DecodeException e){
      LOGGER.warn("Cannot parse body to JsonObject:\n{}", fragment.getBody());
    }
  }
}
