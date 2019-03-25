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
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class FragmentEvent {

  private static final String FRAGMENT_KEY = "fragment";
  private static final String LOG_KEY = "log";
  private static final String STATUS_KEY = "status";
  private static final String ERROR_MESSAGE_KEY = "errorMessage";

  // TODO removed final here
  private Fragment fragment;
  private final EventLog log;
  private Status status = Status.UNPROCESSED;
  private String errorMessage;

  public FragmentEvent(Fragment fragment) {
    this.fragment = fragment;
    this.log = new EventLog();
  }

  public FragmentEvent(FragmentEvent event) {
    this.fragment = event.fragment;
    this.log = event.log;
    this.status = event.status;
    this.errorMessage = event.errorMessage;
  }

  public FragmentEvent(JsonObject json) {
    this.fragment = new Fragment(json.getJsonObject(FRAGMENT_KEY));
    this.log = new EventLog(json.getJsonObject(LOG_KEY));
    this.status = Status.valueOf(json.getString(STATUS_KEY));
    this.errorMessage = json.getString(ERROR_MESSAGE_KEY);
  }

  public FragmentEvent log(EventLogEntry logEntry) {
    log.append(logEntry);
    return this;
  }

  public Fragment getFragment() {
    return fragment;
  }

  public FragmentEvent setFragment(Fragment fragment) {
    this.fragment = fragment;
    return this;
  }

  public JsonObject getLog() {
    return log.toJson();
  }

  public Status getStatus() {
    return status;
  }

  public FragmentEvent setStatus(Status status) {
    this.status = status;
    return this;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(FRAGMENT_KEY, fragment.toJson())
        .put(LOG_KEY, log.toJson())
        .put(STATUS_KEY, status)
        .put(ERROR_MESSAGE_KEY, errorMessage);
  }

  @Override
  public String toString() {
    return "FragmentEvent{" +
        "fragment=" + fragment +
        ", log=" + log +
        ", status=" + status +
        ", errorMessage='" + errorMessage + '\'' +
        '}';
  }

  public enum Status {
    UNPROCESSED, SUCCESS, FAILURE
  }

}
