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
package io.knotx.fragments.task.engine.node;

import static io.knotx.fragments.api.FragmentResult.ERROR_TRANSITION;

import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.api.Node;
import io.knotx.fragments.task.engine.EventLog;
import io.knotx.fragments.task.engine.TaskResult;
import io.knotx.fragments.task.engine.TaskResult.Status;

public class NodeLogger {

  private final EventLog eventLog;

  public NodeLogger(EventLog eventLog) {
    this.eventLog = eventLog;
  }

  void onNodeStart(Node node) {
    eventLog.nodeStarted(node.getId());
  }

  void onCompositeResult(Node node, TaskResult jointResult, String transition) {
    eventLog.appendAll(jointResult.getLog());
    if(jointResult.getStatus() == Status.UNPROCESSED) {
      eventLog.compositeUnprocessed(node.getId(), transition);
    } else {
      if (ERROR_TRANSITION.equals(transition)) {
        eventLog.compositeError(node.getId(), transition);
      } else {
        eventLog.compositeSuccess(node.getId(), transition);
      }
    }
  }

  void onResultDelivered(Node node, FragmentResult fragmentResult) {
    // TODO: other transitions?
    if (ERROR_TRANSITION.equals(fragmentResult.getTransition())) {
      eventLog.error(node.getId(), fragmentResult);
    } else {
      eventLog.success(node.getId(), fragmentResult);
    }
  }

  void onException(Node node, Throwable error) {
    eventLog.exception(node.getId(), ERROR_TRANSITION, error);
  }
}
