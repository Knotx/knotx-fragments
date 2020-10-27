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
package io.knotx.fragments.action.api.invoker;

import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class ActionInvocation {

  public enum Status {
    RESULT_DELIVERED, EXCEPTION, TIMEOUT
  }

  private final long duration;
  private final FragmentResult fragmentResult;
  private final Status status;
  private final Throwable error;

  private ActionInvocation(long duration, FragmentResult fragmentResult,
      Status status, Throwable error) {
    this.duration = duration;
    this.fragmentResult = fragmentResult;
    this.status = status;
    this.error = error;
  }

  public static ActionInvocation resultDelivered(long duration, FragmentResult result) {
    return new ActionInvocation(duration, result, Status.RESULT_DELIVERED, null);
  }

  public static ActionInvocation exception(long duration, Throwable error,
      FragmentContext original) {
    return new ActionInvocation(duration,
        FragmentResult.exception(original, error),
        Status.EXCEPTION,
        error);
  }

  public static ActionInvocation timeout(long duration, FragmentContext original) {
    return new ActionInvocation(duration,
        FragmentResult.externalTimeout(original),
        Status.TIMEOUT,
        new TimeoutException("External timeout reported"));
  }

  public long getDuration() {
    return duration;
  }

  public FragmentResult getFragmentResult() {
    return fragmentResult;
  }

  public Status getStatus() {
    return status;
  }

  public Throwable getError() {
    return error;
  }

  public boolean isResultDelivered() {
    return Status.RESULT_DELIVERED.equals(status);
  }

  public void rethrowIfResultNotDelivered() {
    if (!isResultDelivered()) {
      if(error != null) {
        if(error instanceof RuntimeException) {
          throw (RuntimeException) error;
        } else {
          throw new RuntimeException(error);
        }
      } else {
        throw new RuntimeException("Failed ActionInvocation. Missing error details.");
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActionInvocation that = (ActionInvocation) o;
    return duration == that.duration &&
        Objects.equals(fragmentResult, that.fragmentResult);
  }

  @Override
  public int hashCode() {
    return Objects.hash(duration, fragmentResult);
  }

  @Override
  public String toString() {
    return "ActionInvocation{" +
        "duration=" + duration +
        ", fragmentResult=" + fragmentResult +
        '}';
  }
}
