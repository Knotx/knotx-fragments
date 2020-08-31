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
package io.knotx.fragments.task.factory.generic.node.action.metadata;

import com.google.common.collect.ImmutableMap;

import io.knotx.fragments.action.core.ActionFactoryOptions;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.factory.generic.node.StubNode;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public final class TestConstants {

  private TestConstants() {
    // Constant class
  }

  static final JsonObject FACTORY_CONFIG = new JsonObject()
      .put("some-option", "some-value");

  static final ActionFactoryOptions ALIAS_A_OPTIONS = new ActionFactoryOptions("cb", FACTORY_CONFIG,
      "alias-B");
  static final ActionFactoryOptions ALIAS_B_OPTIONS = new ActionFactoryOptions("cache",
      FACTORY_CONFIG,
      "alias-C");
  static final ActionFactoryOptions ALIAS_C_OPTIONS = new ActionFactoryOptions("http",
      FACTORY_CONFIG);
  static final ActionFactoryOptions ALIAS_MISCONFIGURED_OPTIONS = new ActionFactoryOptions("cache",
      FACTORY_CONFIG, "alias-not-existing");

  static final ActionEntry ALIAS_A = new ActionEntry("alias-A", ALIAS_A_OPTIONS);
  static final ActionEntry ALIAS_B = new ActionEntry("alias-B", ALIAS_B_OPTIONS);
  static final ActionEntry ALIAS_C = new ActionEntry("alias-C", ALIAS_C_OPTIONS);
  static final ActionEntry ALIAS_MISCONFIGURED = new ActionEntry("alias-misconfigured",
      ALIAS_MISCONFIGURED_OPTIONS);
  static final ActionEntry ALIAS_NOT_EXISTING = new ActionEntry("alias-not-existing", null);

  public static final Map<String, Node> EDGES = ImmutableMap.of(
      FragmentResult.SUCCESS_TRANSITION, new StubNode("next-success"),
      FragmentResult.ERROR_TRANSITION, new StubNode("next-error"),
      "_fallback", new StubNode("next-fallback"),
      "_custom", new StubNode("next-custom")
  );

  public static final Map<String, String> TRANSITIONS = ImmutableMap.of(
      FragmentResult.SUCCESS_TRANSITION, "next-success",
      FragmentResult.ERROR_TRANSITION, "next-error",
      "_fallback", "next-fallback",
      "_custom", "next-custom"
  );
}
