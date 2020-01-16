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
package io.knotx.fragments.handler.action.cb;

import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.INFO;
import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.actionlog.ActionLogger;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.eventbus.ReplyFailure;
import io.vertx.rxjava.core.Future;

class CircuitBreakerDoActions {

  static void applySuccess(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    ActionLogger actionLogger = ActionLogger.create("action", INFO);
    actionLogger.info("info", "success");
    Future.succeededFuture(new FragmentResult(fragmentContext.getFragment(), SUCCESS_TRANSITION,
        actionLogger.toLog().toJson()))
        .setHandler(resultHandler);
  }

  static void applyErrorTransition(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    ActionLogger actionLogger = ActionLogger.create("action", INFO);
    actionLogger.info("info", "error");
    Future.succeededFuture(new FragmentResult(fragmentContext.getFragment(), ERROR_TRANSITION,
        actionLogger.toLog().toJson()))
        .setHandler(resultHandler);
  }

  static void applyFailure(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    ActionLogger actionLogger = ActionLogger.create("action", INFO);
    actionLogger.info("info", "failure");
    Future.<FragmentResult>failedFuture(new IllegalStateException("Application failed!")).setHandler(resultHandler);
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
                new FragmentResult(fragmentContext.getFragment(), SUCCESS_TRANSITION)
            ).setHandler(resultHandler));
  }

  static Action applyOneAfterAnother(Action first, Action second) {
    AtomicInteger counter = new AtomicInteger(0);
    return (fragmentContext, resultHandler) -> {
      applySecondAfterFirst(fragmentContext, resultHandler, first, second,
          counter.incrementAndGet());
    };
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
