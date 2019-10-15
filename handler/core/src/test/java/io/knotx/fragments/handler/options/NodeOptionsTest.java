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
package io.knotx.fragments.handler.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.junit5.util.FileReader;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NodeOptionsTest {

  @Test
  @DisplayName("Expect action node options when action is configured")
  void expectActionNodeWhenActionDefined() throws IOException {
    //given
    JsonObject config = from("tasks/actionNode.json");

    //when
    NodeOptions nodeOptions = new NodeOptions(config);

    //then
    assertFalse(nodeOptions.isComposite());
    assertEquals("simple", nodeOptions.getAction());
    assertNull(nodeOptions.getActions());
  }

  @Test
  @DisplayName("Expect composite node options when actions array is configured")
  void expectCompositeNodeWhenActionsDefined() throws IOException {
  //given
    JsonObject config = from("tasks/compositeNode.json");

    //when
    NodeOptions nodeOptions = new NodeOptions(config);

    //then
    assertTrue(nodeOptions.isComposite());
    assertNull(nodeOptions.getAction());
    assertFalse(nodeOptions.getActions().isEmpty());
  }

  @Test
  @DisplayName("Expect action node when both action and actions array are configured")
  void expectActionNodeWhenBothActionAndActionsDefined() throws IOException {
    //given
    JsonObject config = from("tasks/misconfiguredNode.json");

    //when
    NodeOptions nodeOptions = new NodeOptions(config);

    //then
    assertFalse(nodeOptions.isComposite());
    assertEquals("simple", nodeOptions.getAction());
    assertFalse(nodeOptions.getActions().isEmpty());
  }

  private JsonObject from(String fileName) throws IOException {
    return new JsonObject(FileReader.readText(fileName));
  }
}