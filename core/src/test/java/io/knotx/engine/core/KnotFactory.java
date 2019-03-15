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
package io.knotx.engine.core;

import io.knotx.engine.api.FragmentEvent;
import io.knotx.engine.api.FragmentEventResult;
import io.knotx.engine.api.KnotProcessingFatalException;
import io.knotx.engine.api.TraceableKnotOptions;
import io.knotx.fragment.Fragment;
import io.knotx.knotengine.core.junit.MockKnotProxy;
import io.reactivex.Maybe;
import io.reactivex.SingleSource;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

final class KnotFactory {

  private KnotFactory() {
    // hidden constructor
  }

  static void createNotProcessingKnot(Vertx vertx, final String address) {
    MockKnotProxy.register(vertx.getDelegate(), address,
        fragmentContext -> Maybe.empty()
    );
  }

  static void createSuccessKnot(Vertx vertx, String address, String transition) {
    MockKnotProxy.register(vertx.getDelegate(), address,
        fragmentContext ->
        {
          FragmentEvent fragmentEvent = fragmentContext.getFragmentEvent();
          return Maybe.just(new FragmentEventResult(fragmentEvent, transition));
        }
    );
  }

  static void createFailingKnot(Vertx vertx, String address, boolean exitOnError) {
    MockKnotProxy
        .register(vertx.getDelegate(), address,
            new TraceableKnotOptions("next", "error", exitOnError),
            fragmentContext -> {
              Fragment anyFragment = new Fragment("body", new JsonObject(), "");
              throw new KnotProcessingFatalException(anyFragment);
            });
  }

  static void createLongProcessingKnot(Vertx vertx, String address, String transition,
      long delay) {
    MockKnotProxy.register(vertx.getDelegate(), address,
        fragmentContext ->
        {
          FragmentEvent fragmentEvent = fragmentContext.getFragmentEvent();
          SingleSource<FragmentEventResult> emitter =
              singleObserver -> vertx.timerStream(delay)
                  .toObservable()
                  .subscribe(
                      time -> singleObserver
                          .onSuccess(new FragmentEventResult(fragmentEvent, transition))
                  );
          return Maybe.fromSingle(emitter);
        }
    );
  }
}
