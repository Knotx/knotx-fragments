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

import static io.knotx.fragments.action.api.log.ActionLogLevel.INFO;
import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.log.ActionLogger;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.rxjava.core.Future;
import java.util.concurrent.atomic.AtomicInteger;

class CircuitBreakerDoActions {

  public static final String CUSTOM_TRANSITION = "_custom";

  static void applySuccess(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    ActionLogger actionLogger = ActionLogger.create("action", INFO);
    actionLogger.info("info", "success");
    Future.succeededFuture(FragmentResult.success(fragmentContext.getFragment(),
        actionLogger.toLog().toJson()))
        .onComplete(resultHandler);
  }

  static void applyCustomTransition(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    ActionLogger actionLogger = ActionLogger.create("action", INFO);
    actionLogger.info("info", "custom");
    Future.succeededFuture(FragmentResult.success(fragmentContext.getFragment(), CUSTOM_TRANSITION,
        actionLogger.toLog().toJson()))
        .onComplete(resultHandler);
  }

  static void applyErrorTransition(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    ActionLogger actionLogger = ActionLogger.create("action", INFO);
    actionLogger.info("info", "error");
    Future.succeededFuture(FragmentResult.success(fragmentContext.getFragment(), ERROR_TRANSITION,
        actionLogger.toLog().toJson()))
        .onComplete(resultHandler);
  }

  static void applyFailure(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    ActionLogger actionLogger = ActionLogger.create("action", INFO);
    actionLogger.info("info", "failure");
    Future.<FragmentResult>failedFuture(new IllegalStateException("Application failed!"))
        .onComplete(resultHandler);
  }

  static void applyException(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    throw new IllegalStateException("Action throws runtime exception!");
  }

  static void applySuccessDelay(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    Vertx vertx = Vertx.vertx();
    vertx.setTimer(1500,
        l ->
            Future.succeededFuture(
                FragmentResult.success(fragmentContext.getFragment())
            ).onComplete(resultHandler));
  }

  static Action applyOneAfterAnother(Action first, Action second) {
    AtomicInteger counter = new AtomicInteger(0);
    return (ctx, handler) ->
        applySecondAfterFirst(ctx, handler, first, second, counter.incrementAndGet());
  }

  static void applySecondAfterFirst(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler, Action first, Action second,
      Integer counter) {
    if (counter == 1) {
      first.apply(fragmentContext, resultHandler);
    } else {
      second.apply(fragmentContext, resultHandler);
    }
  }
}
