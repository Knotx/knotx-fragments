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
package io.knotx.fragments.engine;


import static io.knotx.fragments.handler.api.domain.FragmentResult.ERROR_TRANSITION;
import static io.knotx.fragments.handler.api.domain.FragmentResult.SUCCESS_TRANSITION;

import io.knotx.fragments.api.Fragment;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import java.util.Optional;

@DataObject
public class FragmentEvent {

  private static final String FRAGMENT_KEY = "fragment";
  private static final String LOG_KEY = "log";
  private static final String STATUS_KEY = "status";
  private static final String DEBUG_DATA_KEY = "debugData";

  private final EventLog log;
  private Fragment fragment;
  private Status status;
  private JsonObject debugData;


  public FragmentEvent(Fragment fragment) {
    this.fragment = fragment;
    this.log = new EventLog();
    this.status = Status.UNPROCESSED;
    this.debugData = new JsonObject();
  }

  public FragmentEvent(JsonObject json) {
    this.fragment = new Fragment(json.getJsonObject(FRAGMENT_KEY));
    this.log = new EventLog(json.getJsonObject(LOG_KEY));
    this.status = Status.valueOf(json.getString(STATUS_KEY));
    this.debugData = json.getJsonObject(DEBUG_DATA_KEY);
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(FRAGMENT_KEY, fragment.toJson())
        .put(LOG_KEY, log.toJson())
        .put(STATUS_KEY, status)
        .put(DEBUG_DATA_KEY, debugData);
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

  public JsonObject getLogAsJson() {
    return log.toJson();
  }

  public EventLog getLog() {
    return log;
  }

  public void appendLog(EventLog log) {
    this.log.appendAll(log);
  }

  public Status getStatus() {
    return status;
  }

  public FragmentEvent setStatus(Status status) {
    this.status = status;
    return this;
  }

  public JsonObject getDebugData() {
    return debugData;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FragmentEvent that = (FragmentEvent) o;
    return Objects.equals(log, that.log) &&
        Objects.equals(fragment, that.fragment) &&
        status == that.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(log, fragment, status);
  }

  @Override
  public String toString() {
    return "FragmentEvent{" +
        "log=" + log +
        ", fragment=" + fragment +
        ", status=" + status +
        '}';
  }

  public enum Status {
    UNPROCESSED,
    SUCCESS(SUCCESS_TRANSITION),
    FAILURE(ERROR_TRANSITION);

    private String defaultTransition;

    Status() {
      //empty constructor
    }

    Status(String defaultTransition) {
      this.defaultTransition = defaultTransition;
    }

    public Optional<String> getDefaultTransition() {
      return Optional.ofNullable(defaultTransition);
    }
  }

}
