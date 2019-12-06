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
package io.knotx.fragments.handler.api.actionlog;

import static io.knotx.fragments.handler.api.actionlog.ActionInvocationLog.error;
import static io.knotx.fragments.handler.api.actionlog.ActionInvocationLog.success;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.vertx.core.json.JsonObject;

class ActionLogBuilder {
  private String alias;
  private JsonObject logs;
  private ArrayList<ActionInvocationLog> doActionLogs;

  ActionLogBuilder(String alias){
    this.alias = alias;
    this.logs = new JsonObject();
    this.doActionLogs = new ArrayList<>();
  }

  ActionLogBuilder appendInvocationLogEntry(long duration, ActionLog actionLog){
    doActionLogs.add(success(duration, actionLog));
    return this;
  }

  ActionLogBuilder appendFailureInvocationLogEntry(long duration, ActionLog actionLog){
    doActionLogs.add(error(duration,  actionLog));
    return this;
  }

  ActionLogBuilder addLog(String key, String value){
    logs.put(key, value);
    return this;
  }

  ActionLogBuilder addLog(String key, JsonObject value){
    logs.put(key, value);
    return this;
  }

  ActionLog build(){
    Stream.of(doActionLogs.toArray());
    return new ActionLog(alias, logs, doActionLogs);
  }
}
