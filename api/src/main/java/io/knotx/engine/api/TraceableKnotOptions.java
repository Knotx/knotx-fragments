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
import java.util.Optional;

public class TraceableKnotOptions {

  private static final String TRANSITIONS_KEY = "transitions";
  private static final String UNPROCESSED_KEY = "unprocessed";
  private static final String ERROR_KEY = "error";
  private static final String EXIT_ON_ERROR_KEY = "exitOnError";

  private static final String DEFAULT_NEXT_KNOT_TRANSITION = "next";
  private static final String DEFAULT_ERROR_TRANSITION = ERROR_KEY;
  private static final boolean DEFAULT_EXIT_ON_ERROR = false;

  private final String unprocessedTransition;
  private final String errorTransition;
  private final boolean exitOnError;

  public TraceableKnotOptions(String unprocessedTransition, String errorTransition,
      boolean exitOnError) {
    this.unprocessedTransition = unprocessedTransition;
    this.errorTransition = errorTransition;
    this.exitOnError = exitOnError;
  }

  public TraceableKnotOptions() {
    this.unprocessedTransition = DEFAULT_NEXT_KNOT_TRANSITION;
    this.errorTransition = DEFAULT_ERROR_TRANSITION;
    this.exitOnError = DEFAULT_EXIT_ON_ERROR;
  }

  public TraceableKnotOptions(JsonObject json) {
    Optional<JsonObject> transitions = Optional.ofNullable(json.getJsonObject(TRANSITIONS_KEY));
    this.unprocessedTransition = transitions
        .map(t -> t.getString(UNPROCESSED_KEY, DEFAULT_NEXT_KNOT_TRANSITION))
        .orElse(DEFAULT_NEXT_KNOT_TRANSITION);
    this.errorTransition = transitions
        .map(t -> t.getString(ERROR_KEY, DEFAULT_ERROR_TRANSITION))
        .orElse(DEFAULT_ERROR_TRANSITION);
    this.exitOnError = json.getBoolean(EXIT_ON_ERROR_KEY, DEFAULT_EXIT_ON_ERROR);
  }

  public JsonObject toJson(JsonObject json) {
    return json.put(TRANSITIONS_KEY,
        new JsonObject().put(UNPROCESSED_KEY, unprocessedTransition)
            .put(ERROR_KEY, errorTransition))
        .put(EXIT_ON_ERROR_KEY, exitOnError);
  }

  public String getUnprocessedTransition() {
    return unprocessedTransition;
  }

  public String getErrorTransition() {
    return errorTransition;
  }

  public boolean isExitOnError() {
    return exitOnError;
  }

  @Override
  public String toString() {
    return "TraceableKnotOptions{" +
        "unprocessedTransition='" + unprocessedTransition + '\'' +
        ", errorTransition='" + errorTransition + '\'' +
        ", exitOnError=" + exitOnError +
        '}';
  }
}
