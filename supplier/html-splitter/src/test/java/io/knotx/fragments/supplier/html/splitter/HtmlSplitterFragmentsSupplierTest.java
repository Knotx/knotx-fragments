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
package io.knotx.fragments.supplier.html.splitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.supplier.api.FragmentsProvisionException;
import io.knotx.server.api.context.ClientResponse;
import io.knotx.server.api.context.RequestContext;
import io.vertx.core.buffer.Buffer;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HtmlSplitterFragmentsSupplierTest {

  @Mock
  private ClientResponse clientResponse;

  @Mock
  private HtmlFragmentSplitter splitter;

  @Mock
  private RequestContext requestContext;

  private HtmlSplitterFragmentsSupplier tested;

  @BeforeEach
  void setUp() {
    tested = new HtmlSplitterFragmentsSupplier(splitter);
    when(requestContext.getClientResponse()).thenReturn(clientResponse);
  }

  @Test
  @DisplayName("Expect FragmentsProvisionException when template body is missing")
  void handleMissingTemplate() {
    // given
    when(clientResponse.getBody()).thenReturn(null);

    // then
    assertThrows(FragmentsProvisionException.class, () -> tested.getFragments(requestContext));
  }

  @Test
  @DisplayName("Expect FragmentsProvisionException when template body is empty")
  void handleEmptyTemplate() {
    // given
    when(clientResponse.getBody()).thenReturn(Buffer.buffer());

    // then
    assertThrows(FragmentsProvisionException.class, () -> tested.getFragments(requestContext));
  }

  @Test
  @DisplayName("Expect list of fragments  when template is not empty")
  void checkPayloadAndClientRequestRewritten() throws FragmentsProvisionException {
    // given
    Buffer buffer = Mockito.mock(Buffer.class);
    when(buffer.toString()).thenReturn("body content");
    when(clientResponse.getBody()).thenReturn(buffer);

    List<Fragment> expectedFragments = Collections.singletonList(Mockito.mock(Fragment.class));
    when(splitter.split("body content")).thenReturn(expectedFragments);

    // when

    List<Fragment> fragments = tested.getFragments(requestContext);

    // then
    assertEquals(expectedFragments, fragments);
  }

  @Test
  @DisplayName("Expect client response body cleared when template is not empty")
  void checkBodyCleared() throws FragmentsProvisionException {
    // given
    Buffer buffer = Mockito.mock(Buffer.class);
    when(buffer.toString()).thenReturn("body content");
    when(clientResponse.getBody()).thenReturn(buffer);

    // when
    tested.getFragments(requestContext);

    // then
    verify(clientResponse).setBody(null);
  }
}