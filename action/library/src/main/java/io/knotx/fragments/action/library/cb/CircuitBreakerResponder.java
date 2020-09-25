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

import static java.lang.String.format;

import io.knotx.fragments.action.api.invoker.ActionInvocation;
import io.knotx.fragments.action.library.exception.DoActionExecuteException;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.Promise;
import java.util.Set;

class CircuitBreakerResponder {

  private final Set<String> errorTransitions;

  CircuitBreakerResponder(Set<String> errorTransitions) {
    this.errorTransitions = errorTransitions;
  }

  void respond(Promise<FragmentResult> promise,
      ActionInvocation invocation, CircuitBreakerActionLogger logger) {
    if (isConsideredSuccess(invocation)) {
      complete(promise, invocation, logger);
    } else {
      fail(promise, invocation, logger);
    }
  }

  private boolean isConsideredSuccess(ActionInvocation invocation) {
    return invocation.isResultDelivered() && !hasErroneousTransition(invocation);
  }

  private boolean isFailedDueToTransition(ActionInvocation invocation) {
    return invocation.isResultDelivered() && hasErroneousTransition(invocation);
  }

  private boolean hasErroneousTransition(ActionInvocation invocation) {
    return errorTransitions.contains(invocation.getFragmentResult().getTransition());
  }

  private void complete(Promise<FragmentResult> promise,
      ActionInvocation invocation, CircuitBreakerActionLogger logger) {
    logger.onSuccess(invocation);
    completePromise(promise, invocation, logger);
  }

  private void fail(Promise<FragmentResult> promise,
      ActionInvocation invocation, CircuitBreakerActionLogger logger) {
    logger.onFailure(invocation);
    failPromise(promise, invocation);
  }

  private void completePromise(Promise<FragmentResult> promise, ActionInvocation invocation,
      CircuitBreakerActionLogger logger) {
    promise.complete(invocation.getFragmentResult().copyWithNewLog(logger.logAsJson()));
  }

  private void failPromise(Promise<FragmentResult> promise, ActionInvocation invocation) {
    if (isFailedDueToTransition(invocation)) {
      promise.fail(new DoActionExecuteException(
          format("Action end up %s transition", invocation.getFragmentResult().getTransition())));
    } else {
      promise.fail(invocation.getError());
    }
  }

}
