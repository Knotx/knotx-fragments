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

import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.handler.api.domain.FragmentContext;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.knotx.fragments.handler.api.exception.ActionFatalException;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

public interface TestFunction extends Function<FragmentContext, Single<FragmentResult>> {

  static TestFunction success() {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      return Single.just(result);
    };
  }

  static TestFunction successWithDelay(long delayInMs) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      return Single.just(result).delay(delayInMs, TimeUnit.MILLISECONDS);
    };
  }

  static TestFunction failure() {
    return fragmentContext -> {
      throw new RuntimeException();
    };
  }

  static TestFunction fatal(Fragment fragment) {
    return fragmentContext -> {
      throw new ActionFatalException(fragment);
    };
  }

  static TestFunction appendPayload(String payloadKey, JsonObject payloadValue) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(payloadKey, payloadValue);
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      return Single.just(result);
    };
  }

  static TestFunction appendPayload(String payloadKey, String payloadValue) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(payloadKey, payloadValue);
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      return Single.just(result);
    };
  }

  static TestFunction appendPayloadBasingOnContext(String expectedPayloadKey,
      String updatedPayloadKey, String updatedPayloadValue) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      String payloadValue = fragment.getPayload().getString(expectedPayloadKey);
      fragment.appendPayload(updatedPayloadKey, payloadValue + updatedPayloadValue);
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      return Single.just(result);
    };
  }

  static TestFunction appendBody(String postfix) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.setBody(fragment.getBody() + postfix);
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      return Single.just(result);
    };
  }

  static TestFunction appendBodyWithPayload(String... expectedPayloadKeys) {
    return fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      for (String expectedPayloadKey : expectedPayloadKeys) {
        String payloadValue = fragment.getPayload().getString(expectedPayloadKey);
        fragment.setBody(fragment.getBody() + payloadValue);
      }
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      return Single.just(result);
    };
  }

}
