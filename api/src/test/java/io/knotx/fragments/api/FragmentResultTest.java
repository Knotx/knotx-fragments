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

import static io.knotx.fragments.api.FragmentOperationFailure.GENERAL_EXCEPTION;
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FragmentResultTest {

  private static final Fragment FRAGMENT = new Fragment("snippet",
      new JsonObject().put("configKey", "configValue"), "body value");
  public static final JsonObject EMPTY_LOG = new JsonObject();
  public static final JsonObject LOG = new JsonObject().put("nodeLogKey", "nodeLogValue");
  public static final String CUSTOM = "custom";

  @Test
  @DisplayName("Expect success transition and log when success")
  void expectSuccessTransitionAndLogWhenSuccess() {
    FragmentResult origin = FragmentResult.success(FRAGMENT, LOG);
    FragmentResult copy = new FragmentResult(origin.toJson());

    assertEquals(origin, copy);
    assertEquals(FRAGMENT, copy.getFragment());
    assertEquals(SUCCESS_TRANSITION, copy.getTransition());
    assertEquals(LOG, copy.getLog());
    assertNull(copy.getError());
  }

  @Test
  @DisplayName("Expect success transition and empty log when success without log")
  void expectSuccessTransitionAndEmptyLogWhenSuccess() {
    FragmentResult origin = FragmentResult.success(FRAGMENT);
    FragmentResult copy = new FragmentResult(origin.toJson());

    assertEquals(origin, copy);
    assertEquals(FRAGMENT, copy.getFragment());
    assertEquals(SUCCESS_TRANSITION, copy.getTransition());
    assertEquals(EMPTY_LOG, copy.getLog());
    assertNull(copy.getError());
  }

  @Test
  @DisplayName("Expect custom transition and log when success with custom transition")
  void expectCustomTransitionAndLogWhenSuccessWithCustomTransition() {
    FragmentResult origin = FragmentResult.success(FRAGMENT, CUSTOM, LOG);
    FragmentResult copy = new FragmentResult(origin.toJson());

    assertEquals(origin, copy);
    assertEquals(FRAGMENT, copy.getFragment());
    assertEquals(CUSTOM, copy.getTransition());
    assertEquals(LOG, copy.getLog());
    assertNull(copy.getError());
  }

  @Test
  @DisplayName("Expect custom transition and empty log when success with custom transition without log")
  void expectCustomTransitionAndEmptyLogWhenSuccess() {
    FragmentResult origin = FragmentResult.success(FRAGMENT, CUSTOM);
    FragmentResult copy = new FragmentResult(origin.toJson());

    assertEquals(origin, copy);
    assertEquals(FRAGMENT, copy.getFragment());
    assertEquals(CUSTOM, copy.getTransition());
    assertEquals(EMPTY_LOG, copy.getLog());
    assertNull(copy.getError());
  }

  @Test
  @DisplayName("Expect error transition and exception details when exception")
  void expectErrorDetails() {
    String errorCode = "errorCode";
    String errorMessage = "errorMessage";
    FragmentResult origin = FragmentResult.fail(FRAGMENT, errorCode, errorMessage);
    FragmentResult copy = new FragmentResult(origin.toJson());

    assertEquals(origin, copy);
    assertEquals(FRAGMENT, copy.getFragment());
    assertEquals(FragmentResult.ERROR_TRANSITION, copy.getTransition());
    assertEquals(errorCode, copy.getError().getCode());
    assertEquals(errorMessage, copy.getError().getMessage());
    assertNull(copy.getLog());
  }

  @Test
  @DisplayName("Expect error transition and exception details when exception")
  void expectExceptionDetails() {
    FragmentResult origin = FragmentResult.fail(FRAGMENT, new IllegalArgumentException());
    FragmentResult copy = new FragmentResult(origin.toJson());

    assertEquals(origin, copy);
    assertEquals(FRAGMENT, copy.getFragment());
    assertEquals(FragmentResult.ERROR_TRANSITION, copy.getTransition());
    assertEquals(GENERAL_EXCEPTION, copy.getError().getCode());
    assertNull(copy.getLog());
  }
}