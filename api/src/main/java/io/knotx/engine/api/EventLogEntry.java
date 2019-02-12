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
 *
 * The code comes from https://github.com/tomaszmichalak/vertx-rx-map-reduce.
 */
package io.knotx.engine.api;

import io.vertx.core.json.JsonObject;

public class EventLogEntry {

  private static final String CONSUMER_KEY = "consumer";
  private static final String ACTION_KEY = "action";
  private static final String TRANSITION_KEY = "transition";
  private static final String TIMESTAMP_KEY = "timestamp";

  private String consumer;
  private EventAction action;
  private String transition;
  private long timestamp;

  public static EventLogEntry received(String consumerAddress) {
    return new EventLogEntry(consumerAddress, EventAction.RECEIVED, null);
  }

  static EventLogEntry processed(String consumerAddress, String transition) {
    return new EventLogEntry(consumerAddress, EventAction.PROCESSED, transition);
  }

  static EventLogEntry skipped(String consumerAddress) {
    return new EventLogEntry(consumerAddress, EventAction.SKIPPED, null);
  }

  public static EventLogEntry error(String consumerAddress, String transition) {
    return new EventLogEntry(consumerAddress, EventAction.ERROR, transition);
  }

  private EventLogEntry(String consumerAddress, EventAction action, String transition) {
    this.consumer = consumerAddress;
    this.action = action;
    this.transition = transition;
    this.timestamp = System.currentTimeMillis();
  }

  EventLogEntry(JsonObject json) {
    this.consumer = json.getString(CONSUMER_KEY);
    this.action = EventAction.valueOf(json.getString(ACTION_KEY));
    this.transition = json.getString(TRANSITION_KEY);
    this.timestamp = json.getLong(TIMESTAMP_KEY);
  }

  JsonObject toJson() {
    return new JsonObject()
        .put(CONSUMER_KEY, consumer)
        .put(ACTION_KEY, action.name())
        .put(TRANSITION_KEY, transition)
        .put(TIMESTAMP_KEY, timestamp);
  }

  @Override
  public String toString() {
    return toJson().encode();
  }

  enum EventAction {
    RECEIVED,
    SKIPPED,
    PROCESSED,
    ERROR
  }

}
