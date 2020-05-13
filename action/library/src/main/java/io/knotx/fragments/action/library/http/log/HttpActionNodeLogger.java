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
package io.knotx.fragments.action.library.http.log;

import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.api.log.ActionLogger;
import io.vertx.core.json.JsonObject;

class HttpActionNodeLogger {

  private static final String REQUEST = "request";
  private static final String RESPONSE = "response";
  private static final String RESPONSE_BODY = "responseBody";

  private ActionLogger actionLogger;

  private HttpActionNodeLogger(ActionLogger actionLogger) {
    this.actionLogger = actionLogger;
  }

  static HttpActionNodeLogger create(String alias, ActionLogLevel actionLogLevel) {
    return new HttpActionNodeLogger(ActionLogger.create(alias, actionLogLevel));
  }

  JsonObject getJsonNodeLog() {
    return actionLogger.toLog().toJson();
  }

  void logRequest(ActionLogLevel level, JsonObject requestData) {
    log(level, REQUEST, requestData);
  }

  void logResponse(ActionLogLevel level, JsonObject responseData) {
    log(level, RESPONSE, responseData);
  }

  void logResponseBody(String responseBody) {
    actionLogger.info(RESPONSE_BODY, responseBody);
  }

  void logError(Throwable throwable) {
    actionLogger.error(throwable);
  }

  private void log(ActionLogLevel logLevel, String key, JsonObject value) {
    if(ActionLogLevel.INFO.equals(logLevel)) {
      actionLogger.info(key, value);
    } else {
      actionLogger.error(key, value);
    }
  }

}
