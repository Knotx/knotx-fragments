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
package io.knotx.fragments.engine;

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentOperation;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.engine.exception.NodeFatalException;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

interface TestFunction extends FragmentOperation {

  static TestFunction success() {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      Future.succeededFuture(result).setHandler(resultHandler);
    };
  }

  static TestFunction successWithDelay(long delayInMs, Vertx vertx) {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      vertx.setTimer(delayInMs, event -> Future.succeededFuture(result).setHandler(resultHandler));
    };
  }

  static TestFunction successWithNodeLog(JsonObject nodeObject) {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION, nodeObject);
      Future.succeededFuture(result).setHandler(resultHandler);
    };
  }

  static TestFunction errorWithNodeLog(JsonObject nodeLog) {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      FragmentResult result = new FragmentResult(fragment, ERROR_TRANSITION, nodeLog);
      Future.succeededFuture(result).setHandler(resultHandler);
    };
  }


  static TestFunction failure() {
    return (fragmentContext, resultHandler) -> Future.<FragmentResult>failedFuture(
        new RuntimeException()).setHandler(resultHandler);
  }

  static TestFunction fatal(Fragment fragment) {
    return (fragmentContext, resultHandler) -> Future.<FragmentResult>failedFuture(
        new NodeFatalException(fragment))
        .setHandler(resultHandler);
  }

  static TestFunction appendPayload(String payloadKey, JsonObject payloadValue) {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(payloadKey, payloadValue);
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      Future.succeededFuture(result).setHandler(resultHandler);
    };
  }

  static TestFunction appendPayload(String payloadKey, String payloadValue) {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.appendPayload(payloadKey, payloadValue);
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      Future.succeededFuture(result).setHandler(resultHandler);
    };
  }

  static TestFunction appendPayloadBasingOnContext(String expectedPayloadKey,
      String updatedPayloadKey, String updatedPayloadValue) {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      String payloadValue = fragment.getPayload().getString(expectedPayloadKey);
      fragment.appendPayload(updatedPayloadKey, payloadValue + updatedPayloadValue);
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      Future.succeededFuture(result).setHandler(resultHandler);
    };
  }

  static TestFunction appendBody(String postfix) {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      fragment.setBody(fragment.getBody() + postfix);
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      Future.succeededFuture(result).setHandler(resultHandler);
    };
  }

  static TestFunction appendBodyWithPayload(String... expectedPayloadKeys) {
    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      for (String expectedPayloadKey : expectedPayloadKeys) {
        String payloadValue = fragment.getPayload().getString(expectedPayloadKey);
        fragment.setBody(fragment.getBody() + payloadValue);
      }
      FragmentResult result = new FragmentResult(fragment, SUCCESS_TRANSITION);
      Future.succeededFuture(result).setHandler(resultHandler);
    };
  }
}