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
import static io.knotx.fragments.api.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.fragments.task.engine.TaskResult;
import io.knotx.fragments.task.engine.TaskResult.Status;

public class NodeResult {

  private final Fragment fragment;
  private final String transition;
  private final Status status;

  public static NodeResult fromSingleResult(FragmentResult result) {
    return new NodeResult(result.getFragment(), result.getTransition(), Status.SUCCESS);
  }

  public static NodeResult fromCompositeResult(TaskResult jointEvent, String transition) {
    return new NodeResult(jointEvent.getFragment(), transition, jointEvent.getStatus());
  }

  public static NodeResult fromEmptyComposite(Fragment original) {
    return new NodeResult(original, SUCCESS_TRANSITION, Status.UNPROCESSED);
  }

  public static NodeResult error(Fragment original) {
    return new NodeResult(original, ERROR_TRANSITION, Status.FAILURE);
  }

  private NodeResult(Fragment fragment, String transition, Status status) {
    this.fragment = fragment;
    this.transition = transition;
    this.status = status;
  }

  public String getTransition() {
    return transition;
  }

  public Status getStatus() {
    return status;
  }

  public Fragment getFragment() {
    return fragment;
  }
}
