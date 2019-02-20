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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.engine.api;

import io.knotx.engine.api.FragmentEvent.Status;
import io.reactivex.Maybe;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class TraceableKnotProxy implements KnotProxy {

  private final Logger LOGGER = LoggerFactory.getLogger(TraceableKnotProxy.class);

  private TraceableKnotOptions options;

  protected TraceableKnotProxy(TraceableKnotOptions options) {
    this.options = options;
  }

  @Override
  public void process(FragmentEventContext context,
      Handler<AsyncResult<FragmentEventResult>> result) {
    Maybe.just(context)
        .map(ctx -> {
          ctx.getFragmentEvent().log(EventLogEntry.received(getAddress()));
          return ctx;
        })
        .flatMap(this::execute)
        .doOnSuccess(this::processed)
        .subscribe(
            // processed
            success -> result.handle(Future.succeededFuture(success)),
            error -> {
              if (options.isExitOnError()) {
                LOGGER.error("Could not process fragment[{}], ends the request processing!",
                    context.getFragmentEvent().getFragment().getId(), error);
                result.handle(Future.failedFuture(
                    new KnotProcessingFatalException(context.getFragmentEvent().getFragment())));
              } else {
                result.handle(Future.succeededFuture(processError(context, error)));
              }
            },
            // not processed
            () -> result.handle(Future.succeededFuture(unprocessed(context)))
        );
  }

  protected abstract Maybe<FragmentEventResult> execute(FragmentEventContext fragmentContext);

  protected abstract String getAddress();

  protected FragmentEventResult processError(FragmentEventContext ctx,
      Throwable error) {
    LOGGER
        .warn("Fragment[{}] processing error, Knot [{}]",
            ctx.getFragmentEvent().getFragment().getId(), getAddress());
    FragmentEvent event = ctx.getFragmentEvent()
        .log(EventLogEntry.error(getAddress(), options.getErrorTransition()))
        .setStatus(Status.FAILURE);
    return new FragmentEventResult(event, options.getErrorTransition());
  }

  private FragmentEventResult processed(FragmentEventResult knotResult) {
    FragmentEvent fragmentEvent = knotResult.getFragmentEvent();
    if (LOGGER.isTraceEnabled()) {
      LOGGER
          .trace("Fragment[{}] processed via Knot [{}] with result [{}]",
              fragmentEvent.getFragment().getId(), getAddress(), knotResult);
    }
    fragmentEvent.log(EventLogEntry.processed(getAddress(), knotResult.getTransition()));
    fragmentEvent.setStatus(Status.SUCCESS);
    return knotResult;
  }

  protected FragmentEventResult unprocessed(FragmentEventContext ctx) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER
          .trace("Fragment[{}] not processed via Knot [{}]",
              ctx.getFragmentEvent().getFragment().getId(), getAddress());
    }
    FragmentEvent event = ctx.getFragmentEvent().log(EventLogEntry.skipped(getAddress()));
    return new FragmentEventResult(event, options.getUnprocessedTransition());
  }


}
