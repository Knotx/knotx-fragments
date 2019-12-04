package io.knotx.fragments.handler.api.actionlog;


import static io.knotx.fragments.handler.api.actionlog.ActionInvocationLog.DO_ACTION_LOG;
import static io.knotx.fragments.handler.api.actionlog.ActionInvocationLog.DURATION;
import static io.knotx.fragments.handler.api.actionlog.ActionInvocationLog.SUCCESS;
import static java.util.Collections.EMPTY_LIST;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

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
    ActionLog al = new ActionLog("alias", new JsonObject(), EMPTY_LIST);
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