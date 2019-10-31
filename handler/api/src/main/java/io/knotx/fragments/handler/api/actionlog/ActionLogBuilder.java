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

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import io.vertx.core.json.JsonObject;

public class ActionLogBuilder {
  private String alias;
  private JsonObject logs;
  private List<ActionLog> doActionLogs;

  public ActionLogBuilder(String alias){
    this.alias = alias;
    this.logs = new JsonObject();
    this.doActionLogs = new ArrayList<>();
  }

  ActionLogBuilder addActionLog(ActionLog actionLog){
    doActionLogs.add(actionLog);
    return this;
  }

  public ActionLogBuilder addLog(String key, String value){
    logs.put(key, value);
    return this;
  }

  ActionLogBuilder addLog(String key, JsonObject value){
    logs.put(key, value);
    return this;
  }

  public ActionLog build(){
    return new ActionLog(alias, logs.copy(), ImmutableList.copyOf(doActionLogs));
  }

}
