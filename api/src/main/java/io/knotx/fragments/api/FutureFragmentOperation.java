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
package io.knotx.fragments.api;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public interface FutureFragmentOperation extends FragmentOperation {

  @Override
  default void apply(FragmentContext fragmentContext, Handler<AsyncResult<FragmentResult>> resultHandler) {
    tryApply(fragmentContext).onComplete(resultHandler);
  }

  default Future<FragmentResult> tryApply(FragmentContext fragmentContext) {
    try {
      return applyWithResult(fragmentContext);
    } catch (Throwable t) {
      return Future.failedFuture(t);
    }
  }

  default Future<FragmentResult> applyWithResult(FragmentContext fragmentContext) {
    Future<FragmentResult> result = apply(fragmentContext);
    if (result != null) {
      return result;
    } else {
      return Future.failedFuture(new IllegalStateException(
          "FutureFragmentOperation " + this.getClass().getName() + " returned a null Future<FragmentResult>."));
    }
  }

  Future<FragmentResult> apply(FragmentContext fragmentContext);
}
