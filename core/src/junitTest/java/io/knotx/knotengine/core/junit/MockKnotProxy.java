/*
 * Copyright (C) 2019 Cognifide Limited
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
package io.knotx.knotengine.core.junit;


import io.knotx.engine.api.FragmentContext;
import io.knotx.engine.api.FragmentEventResult;
import io.knotx.engine.api.KnotProxy;
import io.knotx.engine.api.TraceableKnotOptions;
import io.knotx.engine.api.TraceableKnotProxy;
import io.reactivex.Maybe;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceBinder;
import java.util.function.Function;

public final class MockKnotProxy extends TraceableKnotProxy {

  private final String address;
  private final Function<FragmentContext, Maybe<FragmentEventResult>> knotAction;

  private MockKnotProxy(String address,
      Function<FragmentContext, Maybe<FragmentEventResult>> knotAction) {
    super(new TraceableKnotOptions());
    this.address = address;
    this.knotAction = knotAction;
  }

  private MockKnotProxy(String address,
      TraceableKnotOptions options,
      Function<FragmentContext, Maybe<FragmentEventResult>> knotAction) {
    super(options);
    this.address = address;
    this.knotAction = knotAction;
  }

  public static void register(Vertx vertx, String address) {
    register(vertx, address, null);
  }

  public static void register(Vertx vertx, String address,
      Function<FragmentContext, Maybe<FragmentEventResult>> knotAction) {
    new ServiceBinder(vertx)
        .setAddress(address)
        .register(KnotProxy.class, new MockKnotProxy(address, knotAction));
  }

  public static void register(Vertx vertx, String address, TraceableKnotOptions options,
      Function<FragmentContext, Maybe<FragmentEventResult>> knotAction) {
    new ServiceBinder(vertx)
        .setAddress(address)
        .register(KnotProxy.class, new MockKnotProxy(address, options, knotAction));
  }

  @Override
  protected Maybe<FragmentEventResult> execute(FragmentContext fragmentContext) {
    return knotAction.apply(fragmentContext);
  }

  @Override
  protected String getAddress() {
    return address;
  }
}
