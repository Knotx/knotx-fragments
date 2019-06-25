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
package io.knotx.fragments.supplier.single;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.knotx.fragments.api.Fragment;
import io.knotx.server.api.context.RequestContext;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SingleFragmentSupplierTest {

  @Mock
  private RequestContext requestContext;

  @Test
  @DisplayName("Expect fragment with default values created when no options provided")
  void createFragmentFromEmptyConfig() {
    // given
    SingleFragmentSupplier tested = new SingleFragmentSupplier(null);

    // when
    List<Fragment> fragments = tested.getFragments(requestContext);

    // then
    assertEquals(1, fragments.size());
    Fragment fragment = fragments.get(0);
    assertEquals("", fragment.getType());
    assertEquals("", fragment.getBody());
    assertTrue(fragment.getConfiguration().isEmpty());
    assertTrue(fragment.getPayload().isEmpty());

  }

  @Test
  @DisplayName("Expect fragment with initial values form config created")
  void createFragmentFromConfig() {
    // given
    String expectedType = "test";
    String expectedBody = "initial body";
    JsonObject expectedConfiguration = new JsonObject().put("config1", "value1");
    JsonObject expectedPayload = new JsonObject().put("key", "value");
    JsonObject options = new JsonObject()
        .put("type", expectedType)
        .put("body", expectedBody)
        .put("configuration", expectedConfiguration)
        .put("payload", expectedPayload);

    SingleFragmentSupplier tested = new SingleFragmentSupplier(options);

    // when
    List<Fragment> fragments = tested.getFragments(requestContext);

    // then
    assertEquals(1, fragments.size());
    Fragment fragment = fragments.get(0);
    assertEquals(expectedType, fragment.getType());
    assertEquals(expectedBody, fragment.getBody());
    assertEquals(expectedConfiguration, fragment.getConfiguration());
    assertEquals(expectedPayload, fragment.getPayload());
  }
}