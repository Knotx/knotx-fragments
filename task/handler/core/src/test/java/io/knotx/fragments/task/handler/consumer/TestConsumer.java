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
package io.knotx.fragments.task.handler.consumer;

import io.knotx.fragments.task.handler.log.api.FragmentExecutionLogConsumer;
import io.knotx.fragments.task.handler.log.api.FragmentExecutionLogConsumerFactory;
import io.vertx.core.json.JsonObject;

public class TestConsumer implements FragmentExecutionLogConsumerFactory {

  @Override
  public String getName() {
    return "testConsumer";
  }

  @Override
  public FragmentExecutionLogConsumer create(JsonObject config) {
    return (clientRequest, executionLogs) -> executionLogs
        .forEach(log -> log.getFragment().setBody("testConsumer"));
  }
}
