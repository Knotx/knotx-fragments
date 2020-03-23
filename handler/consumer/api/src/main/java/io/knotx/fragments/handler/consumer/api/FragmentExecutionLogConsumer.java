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
package io.knotx.fragments.handler.consumer.api;

import io.knotx.fragments.handler.consumer.api.model.FragmentExecutionLog;
import io.knotx.server.api.context.ClientRequest;
import java.util.List;

/**
 * Fragment execution log consumer receives {@link FragmentExecutionLog} after the {@link
 * io.knotx.fragments.engine.api.Task} evaluation.
 */
public interface FragmentExecutionLogConsumer {

  /**
   * Gets a list of processed and unprocessed fragments (execution logs).
   *
   * @param request - original request data
   * @param executions - list of fragment execution logs
   */
  void accept(ClientRequest request, List<FragmentExecutionLog> executions);

}
