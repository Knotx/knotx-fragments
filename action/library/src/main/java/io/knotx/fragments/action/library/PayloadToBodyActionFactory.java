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
package io.knotx.fragments.action.library;

import static io.knotx.commons.json.JsonObjectUtil.getJsonObject;
import static io.knotx.fragments.action.library.helper.ValidationHelper.checkArgument;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.api.SyncAction;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Objects;
import java.util.Optional;

public class PayloadToBodyActionFactory implements ActionFactory {

  private static final String KEY = "key";

  @Override
  public String getName() {
    return "payload-to-body";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    checkArgument(getName(), doAction != null, "Payload to body action does not support doAction");

    return (SyncAction) fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      String payloadKey = Objects.nonNull(config) ? config.getString(KEY) : null;

      return getBodyFromPayload(payloadKey, fragment.getPayload())
          .map(body -> toFragmentResult(fragment, body))
          .orElse(new FragmentResult(fragment, FragmentResult.ERROR_TRANSITION));
    };
  }

  private Optional<String> getBodyFromPayload(String key, JsonObject payload) {
    JsonObject body = Objects.isNull(key) ? payload : getJsonObject(key, payload);
    return Optional.ofNullable(body)
        .map(JsonObject::encodePrettily);
  }

  private FragmentResult toFragmentResult(Fragment fragment, String body) {
    fragment.setBody(body);
    return new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION);
  }

}
