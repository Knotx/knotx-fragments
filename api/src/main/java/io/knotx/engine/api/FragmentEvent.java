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
package io.knotx.engine.api;

import io.knotx.fragment.Fragment;
import io.vertx.core.json.JsonObject;

public class FragmentEvent {

  private final Fragment fragment;
  private FragmentEventLog log;
  private Status status = Status.UNPROCESSED;
  private String errorMessage;

  public FragmentEvent(Fragment fragment) {
    this.fragment = fragment;
    log = new FragmentEventLog();
  }

  public FragmentEvent(JsonObject json) {
    this.fragment = new Fragment(json.getJsonObject("fragment"));
    this.log = new FragmentEventLog(json.getJsonObject("log"));
    this.status = Status.valueOf(json.getString("status"));
    this.errorMessage = json.getString("errorMessage");
  }

  public void next(String consumerId, String transition) {
    log.append(consumerId, transition);
  }

  public void success(String consumerId, String transition) {
    status = Status.SUCCESS;
    log.append(consumerId, transition);
  }

  public void failure(String consumerId, String transition, String errorMessage) {
    this.errorMessage = errorMessage;
    status = Status.FAILURE;
    log.append(consumerId, transition);
  }

  public void fatal(String consumerId, String transition, String errorMessage) {
    this.errorMessage = errorMessage;
    status = Status.FATAL;
    log.append(consumerId, transition);
  }

  public Fragment getFragment() {
    return fragment;
  }

  public JsonObject getLog() {
    return log.toJson();
  }

  public Status getStatus() {
    return status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put("fragment", fragment.toJson())
        .put("log", log.toJson())
        .put("status", status)
        .put("errorMessage", errorMessage);
  }

  public enum Status {
    SUCCESS, FAILURE, UNPROCESSED, FATAL
  }
}
