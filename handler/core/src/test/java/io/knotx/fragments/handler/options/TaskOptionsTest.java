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
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.junit5.util.FileReader;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskOptionsTest {

  @Test
  @DisplayName("Expect default Task builder with task containing 'a' action.")
  void expectDefaultTaskBuilder() throws IOException {
    //given
    JsonObject config = from("tasks/actionNode.json");

    //when
    TaskOptions options = new TaskOptions(config);

    //then
    assertEquals("default", options.getBuilder().getName());
    assertTrue(options.getBuilder().getConfig().isEmpty());
    assertEquals("simple", options.getConfig().getAction());
  }

  @Test
  @DisplayName("Expect custom Task builder with task containing 'a' action.")
  void expectCustomTaskBuilder() throws IOException {
    //given
    JsonObject config = from("tasks/actionNodeLong.json");

    //when
    TaskOptions options = new TaskOptions(config);

    //then
    assertEquals("custom", options.getBuilder().getName());
    assertEquals(new JsonObject().put("anyKey", "anyValue"), options.getBuilder().getConfig());
    assertEquals("simple", options.getConfig().getAction());
  }

  private JsonObject from(String fileName) throws IOException {
    return new JsonObject(FileReader.readText(fileName));
  }

}