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

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.supplier.api.FragmentsProvisionException;
import io.knotx.fragments.supplier.api.FragmentsSupplier;
import io.knotx.server.api.context.ClientResponse;
import io.knotx.server.api.context.RequestContext;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class HtmlFragmentsSupplier implements FragmentsSupplier {

  private static final Logger LOGGER = LoggerFactory.getLogger(HtmlFragmentsSupplier.class);
  private static final String MISSING_CLIENT_RESPONSE_BODY = "Template body is missing!";

  private final HtmlFragmentSplitter splitter;

  HtmlFragmentsSupplier(HtmlFragmentSplitter splitter) {
    this.splitter = splitter;
  }

  @Override
  public List<Fragment> getFragments(RequestContext requestContext)
      throws FragmentsProvisionException {
    List<Fragment> fragments;
    ClientResponse clientResponse = requestContext.getClientResponse();
    final String template = Optional.ofNullable(clientResponse.getBody()).map(Buffer::toString)
        .orElse(null);
    if (StringUtils.isNotBlank(template)) {
      fragments = splitter.split(template);
      // ToDo configuration, by default clear body
      clearBody(clientResponse);
    } else {
      LOGGER.warn(MISSING_CLIENT_RESPONSE_BODY);
      throw new FragmentsProvisionException(MISSING_CLIENT_RESPONSE_BODY);
    }
    return fragments;
  }

  private void clearBody(ClientResponse clientResponse) {
    clientResponse.setBody(null);
  }

}
