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
package io.knotx.engine.handler;

import io.knotx.engine.api.KnotFlow;
import io.knotx.engine.api.KnotFlowStep;
import io.knotx.fragment.Fragment;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// TODO implement tests for stepAlias
class KnotFlowProviderTest {

  @Test
  @DisplayName("Expect no flow when a fragment does not declare any flow.")
  void get_whenNoFlowAttribute_emptyKnotFlow() {
    // given
    Fragment fragment = new Fragment("type", new JsonObject(), "body");
    KnotFlowProvider tested = new KnotFlowProvider(Collections.emptyMap(), Collections.emptyMap());

    // when
    Optional<KnotFlow> knotFlow = tested.get(fragment);

    // then
    Assert.assertFalse(knotFlow.isPresent());
  }

  @Test
  @DisplayName("Expect no flow when a fragment declares empty flow configuration entry.")
  void get_whenEmptyFlowAttribute_emptyKnotFlow() {
    // given
    Fragment fragment = new Fragment("type", new JsonObject().put("flow", ""), "body");
    KnotFlowProvider tested = new KnotFlowProvider(Collections.emptyMap(), Collections.emptyMap());

    // when
    Optional<KnotFlow> knotFlow = tested.get(fragment);

    // then
    Assert.assertFalse(knotFlow.isPresent());
  }

  @Test
  void get_whenFlatFlowAttributeWithOneKnot_knotFlowWithAttributeAndNoTransitions() {
    // given
    Fragment fragment = new Fragment("type", new JsonObject().put("flow", "aAddress"), "body");
    KnotFlowProvider tested = new KnotFlowProvider(Collections.emptyMap(),
        Collections.singletonMap("aAddress", new KnotFlowStep("aAddress.address")));

    // when
    Optional<KnotFlow> knotFlow = tested.get(fragment);

    // then
    Assert.assertTrue(knotFlow.isPresent());
    Assert.assertEquals(new KnotFlow("aAddress.address", Collections.emptyMap()), knotFlow.get());
  }

  @Test
  void get_whenFlatFlowAttributeWithTwoKnots_knotFlowWithDefaultNextTransitions() {
    // given
    Fragment fragment = new Fragment("type",
        new JsonObject().put("flow", "aAddress,bAddress"), "body");

    Map<String, KnotFlowStep> steps = new HashMap<>();
    steps.put("aAddress", new KnotFlowStep("aAddress.address"));
    steps.put("bAddress", new KnotFlowStep("bAddress.address"));

    KnotFlowProvider tested = new KnotFlowProvider(Collections.emptyMap(), steps);

    // when
    Optional<KnotFlow> knotFlow = tested.get(fragment);

    // then
    Assert.assertTrue(knotFlow.isPresent());
    Assert.assertEquals(new KnotFlow("aAddress.address", Collections
            .singletonMap("next", new KnotFlow("bAddress.address", Collections.emptyMap()))),
        knotFlow.get());
  }

  @Test
  void get_whenEmptyJsonFlowAttribute_emptyFlowKnot() {
    // given
    Fragment fragment = new Fragment("type",
        new JsonObject().put("flow", "{}"), "body");

    KnotFlowProvider tested = new KnotFlowProvider(Collections.emptyMap(), Collections.emptyMap());

    // when
    Optional<KnotFlow> knotFlow = tested.get(fragment);

    // then
    Assert.assertFalse(knotFlow.isPresent());
  }

  @Test
  void get_whenInvalidJsonFlowAttribute_emptyKnotFlow() {
    // given
    Fragment fragment = new Fragment("type",
        new JsonObject().put("flow", "{\"step\":{\"address\":\"aAddress\"}"), "body");

    KnotFlowProvider tested = new KnotFlowProvider(Collections.emptyMap(), Collections.emptyMap());

    // when
    Optional<KnotFlow> knotFlow = tested.get(fragment);

    // then
    Assert.assertFalse(knotFlow.isPresent());
  }

  @Test
  void get_whenJsonFlowAttribute_knotFlowWithTransitions() {
    // given
    Fragment fragment = new Fragment("type",
        new JsonObject().put("flow",
            "{\"step\":{\"address\":\"aAddress\"},\"onTransition\":{\"go-b\":{\"step\":{\"address\":\"bAddress\"}}}}"),
        "body");

    KnotFlowProvider tested = new KnotFlowProvider(Collections.emptyMap(), Collections.emptyMap());

    // when
    Optional<KnotFlow> knotFlow = tested.get(fragment);

    // then
    Assert.assertTrue(knotFlow.isPresent());
    Assert.assertEquals(new KnotFlow("aAddress", Collections
            .singletonMap("go-b", new KnotFlow("bAddress", Collections.emptyMap()))),
        knotFlow.get());
  }

  @Test
  void get_whenEmptyFlowNameAttribute_emptyKnotFlow() {
    // given
    Fragment fragment = new Fragment("type", new JsonObject().put("flowName", ""), "body");
    KnotFlowProvider tested = new KnotFlowProvider(Collections.emptyMap(), Collections.emptyMap());

    // when
    Optional<KnotFlow> knotFlow = tested.get(fragment);

    // then
    Assert.assertFalse(knotFlow.isPresent());
  }

  @Test
  void get_whenFlowNameAttribute_knotFlowFromConfiguration() {
    // given
    KnotFlow expectedKnotFlow = new KnotFlow("address", Collections.emptyMap());
    Map<String, KnotFlow> flows = Collections.singletonMap("knotFlowName", expectedKnotFlow);
    Fragment fragment = new Fragment("type", new JsonObject().put("flowName", "knotFlowName"),
        "body");
    KnotFlowProvider tested = new KnotFlowProvider(flows, Collections.emptyMap());

    // when
    Optional<KnotFlow> knotFlow = tested.get(fragment);

    // then
    Assert.assertTrue(knotFlow.isPresent());
    Assert.assertEquals(expectedKnotFlow, knotFlow.get());
  }

}