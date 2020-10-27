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
package io.knotx.fragments.action.library.cb;

import static io.knotx.fragments.action.library.cb.CircuitBreakerAction.ERROR_LOG_KEY;
import static io.knotx.fragments.action.library.cb.CircuitBreakerAction.INVOCATION_COUNT_LOG_KEY;
import static java.lang.String.format;
import static java.lang.String.valueOf;

import io.knotx.fragments.action.api.invoker.ActionInvocation;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.action.api.log.ActionLogger;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.atomic.AtomicInteger;

class CircuitBreakerActionLogger {

  private final ActionLogger actionLogger;
  private final AtomicInteger counter = new AtomicInteger();

  static CircuitBreakerActionLogger create(String alias, ActionLogLevel level) {
    return new CircuitBreakerActionLogger(ActionLogger.create(alias, level));
  }

  CircuitBreakerActionLogger(ActionLogger actionLogger) {
    this.actionLogger = actionLogger;
  }

  void onInvocation() {
    this.counter.incrementAndGet();
  }

  void onSuccess(ActionInvocation invocation) {
    actionLogger.info(INVOCATION_COUNT_LOG_KEY, valueOf(counter.get()));
    actionLogger.info(invocation);
  }

  void onFailure(ActionInvocation invocation) {
    actionLogger.error(invocation);
  }

  void onFallback(Throwable throwable) {
    actionLogger.error(INVOCATION_COUNT_LOG_KEY, valueOf(counter.get()));
    actionLogger.error(ERROR_LOG_KEY, format("Exception: %s. %s", throwable.getClass(), throwable.getLocalizedMessage()));
  }

  JsonObject logAsJson() {
    return actionLogger.toLog().toJson();
  }
}
