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

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.supplier.api.FragmentsSupplier;
import io.knotx.server.api.context.RequestContext;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Collections;
import java.util.List;

public class SingleFragmentSupplier implements FragmentsSupplier {

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleFragmentSupplier.class);

  private final SingleFragmentSupplierOptions options;

  SingleFragmentSupplier(JsonObject config) {
    options = new SingleFragmentSupplierOptions(config);
  }

  @Override
  public List<Fragment> getFragments(RequestContext requestContext) {
    LOGGER.debug("Creating new Fragment from options: " + options);
    final Fragment fragment = new Fragment(options.getType(), options.getConfiguration(),
        options.getBody());
    fragment.mergeInPayload(options.getPayload());
    return Collections.singletonList(fragment);
  }
}
