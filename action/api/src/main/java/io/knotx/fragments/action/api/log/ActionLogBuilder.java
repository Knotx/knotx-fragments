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
package io.knotx.fragments.action.api.log;

import static io.knotx.fragments.action.api.log.ActionInvocationLog.error;
import static io.knotx.fragments.action.api.log.ActionInvocationLog.success;

import io.knotx.fragments.action.api.invoker.ActionInvocation;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;

class ActionLogBuilder {

  private final String alias;
  private final JsonObject logs;
  private final ArrayList<ActionInvocationLog> invocations;

  ActionLogBuilder(String alias) {
    this.alias = alias;
    this.logs = new JsonObject();
    this.invocations = new ArrayList<>();
  }

  void appendInvocationLogEntry(ActionInvocation invocation) {
    invocations.add(success(invocation));
  }

  void appendFailureInvocationLogEntry(ActionInvocation invocation) {
    invocations.add(error(invocation));
  }

  void addLog(String key, String value) {
    logs.put(key, value);
  }

  void addLog(String key, JsonObject value) {
    logs.put(key, value);
  }

  void addLog(String key, JsonArray value) {
    logs.put(key, value);
  }

  ActionLog build() {
    return new ActionLog(alias, logs, invocations);
  }

}
