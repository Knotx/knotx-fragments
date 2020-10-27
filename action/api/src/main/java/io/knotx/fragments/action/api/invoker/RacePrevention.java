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
package io.knotx.fragments.action.api.invoker;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.Future;

class RacePrevention {

  static Action wrap(Action action) {
    return (fragmentContext, resultHandler) -> action.apply(fragmentContext, asyncResult -> {
      if (asyncResult == null) {
        resultHandler.handle(
            exception("Null AsyncResult returned by action: " + action.getClass().getName()));
      } else if (asyncResult.succeeded() || asyncResult.failed()) {
        resultHandler.handle(asyncResult);
      } else if (asyncResult instanceof Future) {
        ((Future<FragmentResult>) asyncResult).onComplete(resultHandler);
      } else {
        // Lost race condition
        // For root cause, see: https://github.com/vert-x3/vertx-rx/issues/238
        resultHandler.handle(exception(
            "Lost race condition - handler called with an unresolved AsyncResult by action: "
                + action.getClass().getName()));
      }
    });
  }

  private static Future<FragmentResult> exception(String message) {
    return Future.failedFuture(new IllegalStateException(message));
  }
}
