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
package io.knotx.fragments.action.api.log;


import static io.knotx.fragments.action.api.log.ActionInvocationLog.DO_ACTION_LOG;
import static io.knotx.fragments.action.api.log.ActionInvocationLog.DURATION;
import static io.knotx.fragments.action.api.log.ActionInvocationLog.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.vertx.core.json.JsonObject;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActionInvocationLogTest {

  @Test
  @DisplayName("Expect null actionLog when json does not contain such key")
  void expectNullActionLog() {
    //given
    JsonObject json = new JsonObject().put(DURATION, 1L).put(SUCCESS, true);

    //when
    ActionInvocationLog invocationLog = new ActionInvocationLog(json);

    //then
    assertNull(invocationLog.getDoActionLog());
    assertNull(invocationLog.toJson().getJsonObject(DO_ACTION_LOG));
  }

  @Test
  @DisplayName("Expect not null actionLog when json contains such key")
  void expectNotNullActionLog() {
    //given
    ActionLog al = new ActionLog("alias", new JsonObject(), Collections.emptyList());
    JsonObject json = new JsonObject().put(DURATION, 1L)
        .put(SUCCESS, true)
        .put(DO_ACTION_LOG, al.toJson());

    //when
    ActionInvocationLog invocationLog = new ActionInvocationLog(json);

    //then
    assertNotNull(invocationLog.getDoActionLog());
    assertNotNull(invocationLog.toJson().getJsonObject(DO_ACTION_LOG));
  }
}