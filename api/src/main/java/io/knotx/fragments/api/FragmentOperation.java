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

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.lang.rx.RxGen;

@RxGen(FragmentOperation.class)
@VertxGen
public interface FragmentOperation {

  /**
   * Transforms a fragment into the new one. It passes a fragment result containing the new fragment
   * and a transition (which tells about the status of the fragment transformation (e.g. `_success`
   * or `_error`) to result handler.
   *
   * @param fragmentContext - contains both fragment and client request
   * @param resultHandler - handler that is invoked when the new fragment is ready
   */
  void apply(FragmentContext fragmentContext, Handler<AsyncResult<FragmentResult>> resultHandler);

}
