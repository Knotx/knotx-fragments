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

import static io.knotx.fragments.handler.api.actionlog.ActionLogMode.INFO;

import java.util.function.Function;

import io.vertx.core.json.JsonObject;

public class ActionLogger {

  private final ActionLogMode actionLogMode;
  private final JsonObject log;

  private ActionLogger(ActionLogMode actionLogMode) {
    this.actionLogMode = actionLogMode;
    this.log = new JsonObject();
  }

  public static ActionLogger create(ActionLogMode actionLogMode){
    return new ActionLogger(actionLogMode);
  }

  public void info(String key, JsonObject data){
    if(actionLogMode == INFO){
      log.put(key, data);
    }
  }

  public <T> void info(String key, T data, Function<T, JsonObject> toJsonFunc){
    if(actionLogMode == INFO){
      log.put(key, toJsonFunc.apply(data));
    }
  }

  public void info(String key, String data){
    if(actionLogMode == INFO){
      log.put(key, data);
    }
  }

  public void error(String key, JsonObject data){
    log.put(key, data);
  }

  public void error(String key, String data){
    log.put(key, data);
  }

  public <T>  void error(String key, T data, Function<T, JsonObject> toJsonFunc){
    log.put(key, toJsonFunc.apply(data));
  }

  public JsonObject getLog(){
    return log;
  }
}