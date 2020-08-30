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
import io.vertx.core.json.JsonObject;

import java.util.Objects;

public class ActionEntry {

  public static final String METADATA_ALIAS = "alias";
  public static final String METADATA_ACTION_FACTORY = "actionFactory";
  public static final String METADATA_ACTION_CONFIG = "actionConfig";
  public static final String METADATA_DO_ACTION = "doAction";

  private final String alias;
  private final ActionFactoryOptions options;

  ActionEntry(String alias, ActionFactoryOptions options) {
    this.alias = alias;
    this.options = options;
  }

  public JsonObject toMetadata() {
    if (alias == null) {
      return new JsonObject();
    }

    JsonObject config = new JsonObject()
        .put(METADATA_ALIAS, alias);

    if (options != null) {
      config.put(METADATA_ACTION_FACTORY, options.getFactory())
          .put(METADATA_ACTION_CONFIG, options.getConfig());
    }

    return config;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActionEntry entry = (ActionEntry) o;
    return Objects.equals(alias, entry.alias) &&
        Objects.equals(options, entry.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(alias, options);
  }

  @Override
  public String toString() {
    return "ActionEntry{" +
        "alias='" + alias + '\'' +
        ", options=" + options +
        '}';
  }

}
