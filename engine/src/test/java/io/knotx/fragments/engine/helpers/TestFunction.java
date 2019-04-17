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
package io.knotx.fragments.engine.helpers;

import static io.knotx.fragments.handler.api.fragment.FragmentResult.DEFAULT_TRANSITION;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.handler.api.exception.KnotProcessingFatalException;
import io.knotx.fragments.handler.api.fragment.FragmentContext;
import io.knotx.fragments.handler.api.fragment.FragmentResult;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.util.function.Function;

public interface TestFunction extends Function<FragmentContext, Single<FragmentResult>> {

  static TestFunction successWithDefaultTransition() {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      FragmentResult result = new FragmentResult(fragment, DEFAULT_TRANSITION);
      return Single.just(result);
    };
  }

  static TestFunction failure() {
    return fragmentContext -> {
      throw new RuntimeException();
    };
  }

  static TestFunction fatal(Fragment fragment) {
    return fragmentContext -> {
      throw new KnotProcessingFatalException(fragment);
    };
  }

  static TestFunction appendPayload(String payloadKey, JsonObject payloadValue) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(payloadKey, payloadValue);
      FragmentResult result = new FragmentResult(fragment, DEFAULT_TRANSITION);
      return Single.just(result);
    };
  }

  static TestFunction appendBody(String postfix) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.setBody(fragment.getBody() + postfix);
      FragmentResult result = new FragmentResult(fragment, DEFAULT_TRANSITION);
      return Single.just(result);
    };
  }

}