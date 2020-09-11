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

import static io.knotx.commons.json.JsonObjectUtil.getObject;
import static io.knotx.commons.json.JsonObjectUtil.putValue;
import static io.knotx.commons.validation.ValidationHelper.checkArgument;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.action.api.ActionFactory;
import io.knotx.fragments.action.api.Cacheable;
import io.knotx.fragments.action.api.SyncAction;
import io.knotx.fragments.action.library.exception.ActionConfigurationException;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

/**
 * Reads data from payload under "from" key and puts it under "to" key. Key names can be defined in
 * configuration like:
 * <pre>
 *   config {
 *      from = key1
 *      to = key2
 *    }
 * </pre>
 */
@Cacheable
public class CopyPayloadKeyActionFactory implements ActionFactory {

  @Override
  public String getName() {
    return "copy-payload-key";
  }

  @Override
  public Action create(String alias, JsonObject config, Vertx vertx, Action doAction) {
    String from = config.getString("from");
    String to = config.getString("to");

    checkArgument(doAction != null, () -> new ActionConfigurationException(alias,
        "CopyPayloadKey action does not accept doAction"));
    checkArgument(StringUtils.isEmpty(from), () -> new ActionConfigurationException(alias,
        "CopyPayloadKey action requires from property configured"));
    checkArgument(StringUtils.isEmpty(to), () -> new ActionConfigurationException(alias,
        "CopyPayloadKey action requires to property configured"));

    return (SyncAction) fragmentContext -> {
      Fragment fragment = fragmentContext.getFragment();
      copyInPayload(fragment, from, to);
      return FragmentResult.success(fragment);
    };
  }

  private static void copyInPayload(Fragment fragment, String from, String to) {
    JsonObject payload = fragment.getPayload();
    copy(payload, from, to);
    fragment.mergeInPayload(payload);
  }

  private static void copy(JsonObject subject, String from, String to) {
    Object exposedData = getObject(from, subject);
    if (exposedData != null) {
      putValue(to, subject, exposedData);
    }
  }
}

