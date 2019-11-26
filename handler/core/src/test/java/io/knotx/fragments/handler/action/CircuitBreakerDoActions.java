package io.knotx.fragments.handler.action;

import static io.knotx.fragments.handler.api.actionlog.ActionLogLevel.INFO;
import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

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
    Future.succeededFuture(new FragmentResult(fragmentContext.getFragment(), SUCCESS_TRANSITION))
        .setHandler(resultHandler);
  }

  static void applySuccessWithActionLogs(FragmentContext fragmentContext,
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
    actionLogger.info("info", "error");
    Future.<FragmentResult>failedFuture(new IllegalStateException()).setHandler(resultHandler);
  }

  static void applyException(FragmentContext fragmentContext,
      Handler<AsyncResult<FragmentResult>> resultHandler) {
    throw new ReplyException(ReplyFailure.RECIPIENT_FAILURE, "Error from action");
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

  static Action applyTimeout(Vertx vertx) {
    return (fragmentContext, resultHandler) ->
        vertx.setTimer(1500,
            l ->
                Future.succeededFuture(
                    new FragmentResult(fragmentContext.getFragment(), SUCCESS_TRANSITION)
                ).setHandler(resultHandler));
  }
}
