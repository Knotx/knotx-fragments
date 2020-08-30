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
package io.knotx.fragments.task.factory.generic.node.action.metadata;

import io.knotx.fragments.action.core.ActionFactoryOptions;
import io.knotx.fragments.task.factory.api.metadata.OperationMetadata;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;

class OperationMetadataProvider {

  private final String nodeFactoryName;
  private final ActionLookup actionLookup;

  static OperationMetadataProvider create(String nodeFactoryName,
                                                 Map<String, ActionFactoryOptions> aliasToOptions) {
    return new OperationMetadataProvider(nodeFactoryName, new ActionLookup(aliasToOptions));
  }

  OperationMetadataProvider() {
    this.nodeFactoryName = StringUtils.EMPTY;
    this.actionLookup = new ActionLookup();
  }

  OperationMetadataProvider(String nodeFactoryName, ActionLookup actionLookup) {
    this.nodeFactoryName = nodeFactoryName;
    this.actionLookup = actionLookup;
  }

  OperationMetadata provideFor(String alias) {
    return new OperationMetadata(nodeFactoryName, getConfig(alias));
  }

  private JsonObject getConfig(String alias) {
    Builder builder = new Builder();
    for(ActionEntry entry : actionLookup.doActionsFrom(alias)) {
      builder.append(entry);
    }
    return builder.build();
  }

  private static class Builder {

    private final JsonObject root = new JsonObject();
    private JsonObject last = root;

    void append(ActionEntry entry) {
      JsonObject metadata = entry.toMetadata();
      if (isFirst()) {
        root.mergeIn(metadata);
      } else {
        last.put(ActionEntry.METADATA_DO_ACTION, metadata);
        last = metadata;
      }
    }

    JsonObject build() {
      return root;
    }

    private boolean isFirst() {
      return root.isEmpty();
    }
  }

}
