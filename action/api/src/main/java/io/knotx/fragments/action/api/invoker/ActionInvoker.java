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

import static java.time.Instant.now;

import io.knotx.fragments.action.api.Action;
import io.knotx.fragments.api.FragmentContext;
import io.knotx.fragments.api.FragmentResult;
import io.knotx.reactivex.fragments.api.FragmentOperation;
import io.reactivex.Single;

public final class ActionInvoker {

  private ActionInvoker() {
    // utility class
  }

  /**
   * Executes the given action with the given context. Measures execution time and handles errors
   * captured via ReactiveX interface.
   * <p>
   * The downstream <b>will receive a succeeded <code>Single</code></b> with an
   * <code>ActionInvocation</code> object with a delivered <code>FragmentResult</code> and
   * <code>duration</code> in ms in these scenarios:
   * <table cellspacing="10" summary="Happy path execution cases">
   *    <tr><td><b>Action</b></td><td><b>ResultHandler</b></td><td><b>AsyncResult</b></td><td><b>FragmentResult</b></td></tr>
   *    <tr><td>returns</td><td>called (eventually)</td><td>succeeded</td><td>+</td></tr>
   *    <tr><td>returns</td><td>called (eventually)</td><td>succeeding Future (eventually)</td><td>+</td></tr>
   * </table>
   * <i>Please note that only the first case is considered valid in Knot.x environment.</i>
   * To conform to this standard, please use {@link io.knotx.fragments.action.api.SyncAction SyncAction}, {@link io.knotx.fragments.action.api.FutureAction FutureAction} or {@link io.knotx.fragments.action.api.SingleAction SingleAction}.
   * <p>
   * The downstream <b>will also receive a succeeded <code>Single</code></b> with an
   * <code>ActionInvocation</code> object representing a failed invocation, containing exception details, exception-like <code>FragmentResult</code> with original <code>FragmentContext</code> and <code>duration</code> in ms in these scenarios:
   * <table cellspacing="10" summary="Failed ActionInvocation return cases">
   *   <tr><td><b>Action</b></td><td><b>ResultHandler</b></td><td><b>AsyncResult</b></td><td><b>FragmentResult</b></td></tr>
   *   <tr><td>returns</td><td>called (eventually)</td><td>failed</td><td>-</td></tr>
   *   <tr><td>returns</td><td>called (eventually)</td><td>succeeded</td><td>null</td></tr>
   *   <tr><td>returns</td><td>called (eventually)</td><td>failing Future (eventually)</td><td>-</td></tr>
   *   <tr><td>returns</td><td>called (eventually)</td><td>succeeding Future (eventually)</td><td>null</td></tr>
   *   <tr><td>returns</td><td>called (eventually)</td><td>unresolved yet, not Future</td><td>*</td></tr>
   *   <tr><td>returns</td><td>called (eventually)</td><td>null</td><td>-</td></tr>
   *   <tr><td>throws</td><td>not called</td><td>-</td><td>-<td></tr>
   *   <tr><td>null</td><td>not called</td><td>-</td><td>-<td></tr>
   * </table>
   * <i>Please note that only the first and second cases are considered valid in Knot.x environment.</i>
   * <p>
   * The downstream <b>will halt forever</b> (unless terminated by an external timeout) in these scenarios:
   * <table cellspacing="10" summary="Hanging cases">
   *   <tr><td><b>Action</b></td><td><b>ResultHandler</b></td><td><b>AsyncResult</b></td><td><b>FragmentResult</b></td></tr>
   *   <tr><td>returns</td><td>not called</td><td>-</td><td>-</td></tr>
   *   <tr><td>returns</td><td>called (eventually)</td><td>Future (never resolved)</td><td>-</td></tr>
   * </table>
   * <p>
   * <i>Please note that only the first case is considered valid in Knot.x environment.</i>
   * <p>
   * There is also a possibility of a broken ReactiveX stream in case of a fatal VM exception.
   * In this case, the thread executing the stream will be propagated with an exception instead of populating it downstream.
   * This is very unlikely to happen. For reference, see {@link io.reactivex.exceptions.Exceptions#throwIfFatal(Throwable)}
   *
   * @param action  the action to be executed
   * @param context the context with which action will be applied.
   * @return Single representing invocation, that either succeeds or halts.
   */
  public static Single<ActionInvocation> rxApply(Action action, FragmentContext context) {
    Timer timer = new Timer();
    return rxApply(action, context, timer)
        .onErrorReturn(e -> ActionInvocation.exception(timer.duration(), e, context));
  }

  private static Single<ActionInvocation> rxApply(Action action, FragmentContext context,
      Timer timer) {
    if (action == null) {
      return Single.error(new IllegalStateException("Called ActionInvoker with a null action!"));
    }
    return Single.just(action)
        .map(RacePrevention::wrap)
        .map(FragmentOperation::newInstance)
        .doOnEvent((v, e) -> timer.start())
        .flatMap(operation -> operation.rxApply(context))
        // Null FragmentResult is considered an exception.
        // We check it manually as flatMap does not, unlike other RxJava 2 methods
        .doOnSuccess(result -> requireNonNull(result, action))
        .doOnEvent((v, e) -> timer.end())
        .map(fr -> ActionInvocation.resultDelivered(timer.duration(), fr));
  }

  private static void requireNonNull(FragmentResult result, Action action) {
    if (result == null) {
      throw new IllegalStateException(
          "Null FragmentResult delivered by action: " + action.getClass().getName());
    }
  }

  private static class Timer {

    private long startTime;
    private long endTime;

    void start() {
      startTime = now().toEpochMilli();
    }

    void end() {
      endTime = now().toEpochMilli();
    }

    long duration() {
      return endTime - startTime;
    }
  }

}
