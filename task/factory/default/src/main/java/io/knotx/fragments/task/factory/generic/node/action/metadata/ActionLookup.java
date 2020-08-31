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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class ActionLookup {

  private final Map<String, ActionFactoryOptions> aliasToConfig;

  ActionLookup() {
    this.aliasToConfig = Collections.emptyMap();
  }

  ActionLookup(Map<String, ActionFactoryOptions> aliasToConfig) {
    this.aliasToConfig = aliasToConfig;
  }

  Iterable<ActionEntry> doActionsFrom(String rootAlias) {
    return () -> new Iterator<ActionEntry>() {

      private final Set<String> visited = new HashSet<>();
      private String currentAlias = rootAlias;

      @Override
      public boolean hasNext() {
        return currentAlias != null;
      }

      @Override
      public ActionEntry next() {
        visited.add(currentAlias);
        String alias = currentAlias;
        ActionFactoryOptions options = aliasToConfig.get(currentAlias);
        currentAlias = nextAlias(options);
        return new ActionEntry(alias, options);
      }

      private String nextAlias(ActionFactoryOptions options) {
        return Optional.ofNullable(options)
            .map(ActionFactoryOptions::getDoAction)
            .filter(doAction -> !visited.contains(doAction))
            .orElse(null);
      }
    };
  }
}