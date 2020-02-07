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
package io.knotx.fragments.task.factory.node;

import io.knotx.fragments.engine.graph.Node;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.vertx.core.json.JsonObject;
import java.util.Map;

public abstract class NodeWithMetadata implements Node {

  protected final JsonObject metadata = new JsonObject();

  public abstract JsonObject generateMetadata();

  protected void determineStatus() {
    if (!metadata.containsKey("status")) {
      String status;
      if (!metadata.containsKey("response")) {
        status = "unprocessed";
      } else {
        switch (metadata.getJsonObject("response").getString("transition")) {
          case FragmentResult.SUCCESS_TRANSITION:
            status = "success";
            break;
          case FragmentResult.ERROR_TRANSITION:
            status = "error";
            break;
          default:
            status = "other";
        }
      }
      metadata.put("status", status);
    }
  }

  protected void determineMissingChildren(
      Map<String, NodeWithMetadata> edges) {
    if(metadata.containsKey("response")) {
      String transition = metadata.getJsonObject("response").getString("transition");
      if (!edges.containsKey(transition)) {
        edges.put(transition, new MissingNode());
      }
    }
  }

}
