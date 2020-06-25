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
package io.knotx.fragments.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.knotx.fragments.api.Fragment;
import io.knotx.server.api.context.ClientRequest;
import io.knotx.server.api.context.RequestEvent;
import io.knotx.server.api.handler.RequestEventHandlerResult;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

@ExtendWith(MockitoExtension.class)
public class FragmentsAssemblerHandlerTest {

  @Mock
  private ClientRequest clientRequest;

  @Mock
  private RoutingContext routingContext;

  @Test
  @DisplayName("Expect IllegalStateException when no fragments in the routing context")
  public void callAssemblerWithNoFragments_expectIllegalStateException() {
    // given
    FragmentsAssemblerHandler assemblerHandler = new FragmentsAssemblerHandler();
    RequestEvent requestEvent = new RequestEvent(clientRequest);

    // then
    assertThrows(IllegalStateException.class, () -> {
      //when
      assemblerHandler.joinFragmentsBodies(routingContext, requestEvent);
    });
  }

  @Test
  @DisplayName("Expect empty client response body when empty fragments list in the routing context")
  public void callAssemblerWithFragment_expectEmptyBody() {
    // given
    FragmentsAssemblerHandler assemblerHandler = new FragmentsAssemblerHandler();
    when(routingContext.get("fragments")).thenReturn(Collections.emptyList());
    RequestEvent requestEvent = new RequestEvent(clientRequest, new JsonObject());

    // when
    RequestEventHandlerResult result = assemblerHandler.joinFragmentsBodies(routingContext, requestEvent);

    // then
    assertTrue(result.getRequestEvent().isPresent());
    assertEquals(0, result.getBody().length());
    assertNull(result.getStatusCode());
    assertEquals("0", result.getHeaders().get(HttpHeaders.CONTENT_LENGTH));
  }

  @Test
  @DisplayName("Expect fragments body merged to client response body and no status when fragments present in the routing context")
  public void callAssemblerWithFragment_expectAssemblerResultWithBodyAndNoStatus() {
    // given
    String expectedBody = "<h1>Some text</h1>\n" + "<p>Some text</p>";
    FragmentsAssemblerHandler assemblerHandler = new FragmentsAssemblerHandler();

    List<Fragment> fragments = Collections
        .singletonList(new Fragment("_STATIC", new JsonObject(), expectedBody));
    when(routingContext.get("fragments")).thenReturn(fragments);

    RequestEvent requestEvent = new RequestEvent(clientRequest, new JsonObject());

    // when
    RequestEventHandlerResult result = assemblerHandler.joinFragmentsBodies(routingContext, requestEvent);

    // then
    assertTrue(result.getRequestEvent().isPresent());
    assertEquals(Buffer.buffer(expectedBody), result.getBody());
    assertNull(result.getStatusCode());
    assertEquals(Integer.toString((expectedBody.length())),
        result.getHeaders().get(HttpHeaders.CONTENT_LENGTH));
  }

}
