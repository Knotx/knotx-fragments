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

import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_A;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_A_OPTIONS;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_B;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_B_OPTIONS;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_C;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_C_OPTIONS;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_MISCONFIGURED;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_MISCONFIGURED_OPTIONS;
import static io.knotx.fragments.task.factory.generic.node.action.metadata.TestConstants.ALIAS_NOT_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.knotx.fragments.action.core.ActionFactoryOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class ActionLookupTest {

  private static final Map<String, ActionFactoryOptions> ALIAS_TO_CONFIG = new HashMap<>();

  static {
    ALIAS_TO_CONFIG.put("alias-A", ALIAS_A_OPTIONS);
    ALIAS_TO_CONFIG.put("alias-B", ALIAS_B_OPTIONS);
    ALIAS_TO_CONFIG.put("alias-C", ALIAS_C_OPTIONS);
    ALIAS_TO_CONFIG.put("alias-null-options", null);
    ALIAS_TO_CONFIG.put("alias-misconfigured", ALIAS_MISCONFIGURED_OPTIONS);
  }

  @Test
  @DisplayName("Expect no elements when initial alias null")
  void initialAliasNull() {
    ActionLookup tested = new ActionLookup(ALIAS_TO_CONFIG);

    Iterator<ActionEntry> iterator = tested.doActionsFrom(null).iterator();

    assertFalse(iterator.hasNext());
  }

  @Test
  @DisplayName("Expect single element when ActionFactoryOptions null")
  void optionsNull() {
    ActionLookup tested = new ActionLookup(ALIAS_TO_CONFIG);

    Iterator<ActionEntry> iterator = tested.doActionsFrom("alias-null-options").iterator();

    assertEquals(new ActionEntry("alias-null-options", null), iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  @DisplayName("Expect single element when no entry in lookup")
  void noEntry() {
    ActionLookup tested = new ActionLookup(ALIAS_TO_CONFIG);

    Iterator<ActionEntry> iterator = tested.doActionsFrom("alias-not-existing").iterator();

    assertEquals(ALIAS_NOT_EXISTING, iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  @DisplayName("Expect one element when doAction not specified")
  void noDoActionSpecified() {
    ActionLookup tested = new ActionLookup(ALIAS_TO_CONFIG);

    Iterator<ActionEntry> iterator = tested.doActionsFrom("alias-C").iterator();

    assertEquals(ALIAS_C, iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  @DisplayName("Expect two elements when doAction not in lookup")
  void doActionNotInLookup() {
    ActionLookup tested = new ActionLookup(ALIAS_TO_CONFIG);

    Iterator<ActionEntry> iterator = tested.doActionsFrom("alias-misconfigured").iterator();

    assertEquals(ALIAS_MISCONFIGURED, iterator.next());
    assertEquals(ALIAS_NOT_EXISTING, iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  @DisplayName("Expect two elements in valid chain")
  void validTwoElementChain() {
    ActionLookup tested = new ActionLookup(ALIAS_TO_CONFIG);

    Iterator<ActionEntry> iterator = tested.doActionsFrom("alias-B").iterator();

    assertEquals(ALIAS_B, iterator.next());
    assertEquals(ALIAS_C, iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  @DisplayName("Expect three elements in valid chain")
  void validThreeElementChain() {
    ActionLookup tested = new ActionLookup(ALIAS_TO_CONFIG);

    Iterator<ActionEntry> iterator = tested.doActionsFrom("alias-A").iterator();

    assertEquals(ALIAS_A, iterator.next());
    assertEquals(ALIAS_B, iterator.next());
    assertEquals(ALIAS_C, iterator.next());
    assertFalse(iterator.hasNext());
  }

}
