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
package io.knotx.fragments.handler.debug;

import static io.knotx.fragments.api.Fragment.JSON_OBJECT_TYPE;

import java.util.List;

import com.google.common.base.Preconditions;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.engine.FragmentEvent;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

class JsonObjectDebugModeStrategy implements FragmentsDebugModeStrategy{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(JsonObjectDebugModeStrategy.class);

  @Override
  public void updateBodyWithDebugData(JsonObject debugData, List<FragmentEvent> fragmentEvents) {
    fragmentEvents.stream().map(FragmentEvent::getFragment).forEach(f -> addDebugData(debugData, f));
  }

  private void addDebugData(JsonObject debugData, Fragment fragment){
    Preconditions.checkArgument(JSON_OBJECT_TYPE.equals(fragment.getType()));

    try {
      JsonObject body = new JsonObject(fragment.getBody());
      body.put("debug", debugData);
      fragment.setBody(body.encode());
    }catch (DecodeException e){
      LOGGER.error("Cannot parse body to JsonObject:\n{}", fragment.getBody());
    }
  }
}
