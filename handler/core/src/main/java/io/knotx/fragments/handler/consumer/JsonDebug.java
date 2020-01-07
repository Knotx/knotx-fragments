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

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class JsonDebug implements FragmentEventsConsumer {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(JsonDebug.class);

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
