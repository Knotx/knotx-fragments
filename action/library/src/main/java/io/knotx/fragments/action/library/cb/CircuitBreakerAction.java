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

import static io.knotx.fragments.action.api.invoker.ActionInvoker.rxApply;
import static io.knotx.fragments.action.library.cb.CircuitBreakerActionFactory.FALLBACK_TRANSITION;
import static java.lang.String.format;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.FutureAction;
import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.Set;

class CircuitBreakerAction implements FutureAction {

  static final String INVOCATION_COUNT_LOG_KEY = "invocationCount";
  static final String ERROR_LOG_KEY = "error";

  private final String alias;
  private final CircuitBreaker circuitBreaker;
  private final Action doAction;
  private final CircuitBreakerResponder responder;
  private final ActionLogLevel actionLogLevel;

  CircuitBreakerAction(CircuitBreaker circuitBreaker, Action doAction, String alias,
      ActionLogLevel actionLogLevel, Set<String> errorTransitions) {
    this(circuitBreaker, doAction, alias, actionLogLevel, new CircuitBreakerResponder(errorTransitions));
  }

  CircuitBreakerAction(CircuitBreaker circuitBreaker, Action doAction, String alias,
      ActionLogLevel actionLogLevel, CircuitBreakerResponder responder) {
    this.alias = alias;
    this.circuitBreaker = circuitBreaker;
    this.doAction = doAction;
    this.responder = responder;
    this.actionLogLevel = actionLogLevel;
  }

  @Override
  public Future<FragmentResult> applyForFuture(FragmentContext context) {
    CircuitBreakerActionLogger logger = CircuitBreakerActionLogger.create(alias, actionLogLevel);
    return circuitBreaker.executeWithFallback(
        promise -> executeCommand(promise, context, logger),
        throwable -> returnFallback(throwable, context, logger)
    );
  }

  private void executeCommand(Promise<FragmentResult> promise, FragmentContext context,
      CircuitBreakerActionLogger logger) {
    logger.onInvocation();
    rxApply(doAction, context)
        .subscribe(invocation -> responder.respond(promise, invocation, logger));
  }

  private static FragmentResult returnFallback(Throwable throwable,
      FragmentContext context, CircuitBreakerActionLogger logger) {
    logger.onFallback(throwable);
    return FragmentResult.success(context.getFragment(),
        FALLBACK_TRANSITION,
        logger.logAsJson());
  }
}
