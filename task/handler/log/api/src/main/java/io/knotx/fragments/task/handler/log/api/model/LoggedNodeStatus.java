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
package io.knotx.fragments.handler.consumer.api.model;

public enum LoggedNodeStatus {

  /**
   * Node ends with the <code>`_success`</code> <a href="https://github.com/Knotx/knotx-fragments/tree/master/engine#transition">transition</a>.
   */
  SUCCESS,

  /**
   * Node ends with the <code>_error</code> <a href="https://github.com/Knotx/knotx-fragments/tree/master/engine#transition">transition</a>
   * or timeout occurs.
   */
  ERROR,

  /**
   * Node ends with a custom <a href="https://github.com/Knotx/knotx-fragments/tree/master/engine#transition">transition</a>
   * that is declared in a task definition.
   */
  OTHER,

  /**
   * See the <a href="https://github.com/Knotx/knotx-fragments/tree/feature/html-consumer-docuemntation-update/handler/consumer/html#missing-nodes">missing</a>
   * node documentation.
   */
  MISSING,

  /**
   * Previous node has ended with transition pointing to a different node.
   */
  UNPROCESSED;

}
