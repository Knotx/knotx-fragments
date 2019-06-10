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
package io.knotx.fragments.handler.action;

import static io.vertx.core.Future.succeededFuture;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Splitter;

import io.knotx.fragment.Fragment;
import io.knotx.fragments.handler.api.Action;
import io.knotx.fragments.handler.api.ActionFactory;
import io.knotx.fragments.handler.api.domain.FragmentResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;


public class PayloadToBodyActionFactory implements ActionFactory {
  private static final String KEY = "key";

  @Override
  public String getName() {
    return "payload-to-body";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    checkArgument(doAction != null, "Payload to body action does not support doAction");

    return (fragmentContext, resultHandler) -> {
      Fragment fragment = fragmentContext.getFragment();
      String payloadKey = Objects.nonNull(config) ? config.getString(KEY) : null;

      FragmentResult result = getBodyFromPayload(payloadKey, fragment.getPayload())
          .map(body -> toFragmentResult(fragment, body))
          .orElse(new FragmentResult(fragment, FragmentResult.ERROR_TRANSITION));

      Future<FragmentResult> resultFuture = succeededFuture(result);
      resultFuture.setHandler(resultHandler);
    };
  }

  private FragmentResult toFragmentResult(Fragment fragment, String body) {
    fragment.setBody(body);
    return new FragmentResult(fragment, FragmentResult.SUCCESS_TRANSITION);
  }

  private Optional<String> getBodyFromPayload(String key, JsonObject payload){
    JsonObject body = Objects.isNull(key) ? payload : getJsonObject(key, payload);
    return Optional.ofNullable(body)
        .map(JsonObject::encodePrettily);

  }

  private JsonObject getJsonObject(String key, JsonObject parent){
    int dotIndex = key.indexOf('.');

    if(dotIndex == -1){
      return parent.getJsonObject(key);
    }

    String newKey = key.substring(dotIndex + 1);
    JsonObject child = getChild(key, parent, dotIndex);

    return child == null ? null : getJsonObject(newKey, child);
  }

  private JsonObject getChild(String key, JsonObject parent, int dotIndex) {
    String childKey = key.substring(0, dotIndex);
    return parent.getJsonObject(childKey);
  }

  private void checkArgument(boolean condition, String message){
    if(condition){
      throw new IllegalArgumentException(message);
    }
  }
}