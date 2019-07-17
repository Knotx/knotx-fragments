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
package io.knotx.fragments.api;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a small piece of a request that may be processed independently.
 */
@DataObject
public class Fragment {

  public static final String SNIPPET_TYPE = "snippet";
  public static final String JSON_OBJECT_TYPE = "JsonObject";

  private static final int DEBUG_MAX_FRAGMENT_CONTENT_LOG_LENGTH = 256;

  private static final String ID_KEY = "id";
  private static final String TYPE_KEY = "type";
  private static final String CONFIGURATION_KEY = "configuration";
  private static final String BODY_KEY = "body";
  private static final String PAYLOAD_KEY = "payload";

  private final String id;
  private final String type;
  private final JsonObject configuration;
  private String body;
  private final JsonObject payload;

  public Fragment(String type, JsonObject configuration, String body) {
    this.id = UUID.randomUUID().toString();
    this.type = type;
    this.configuration = configuration;
    this.body = body;
    this.payload = new JsonObject();
  }

  public Fragment(JsonObject json) {
    this.id = json.getString(ID_KEY);
    this.type = json.getString(TYPE_KEY);
    this.configuration = json.getJsonObject(CONFIGURATION_KEY);
    this.body = json.getString(BODY_KEY);
    this.payload = json.getJsonObject(PAYLOAD_KEY);
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(ID_KEY, id)
        .put(TYPE_KEY, type)
        .put(CONFIGURATION_KEY, configuration)
        .put(BODY_KEY, body)
        .put(PAYLOAD_KEY, payload);
  }

  /**
   * Unique identifier of the Fragment. Its representaion is currently a {@code String}
   * representation of {@code UUID}. It can never change during processing.
   *
   * @return id of the Fragment.
   */
  public String getId() {
    return id;
  }

  /**
   * Type of a Fragment. Different types of Fragments can be processed in separate ways. Example
   * type could be {@code snippet}. It can never change during processing.
   *
   * @return type of a Fragment.
   */
  public String getType() {
    return type;
  }

  /**
   * Configuration containing all information necessary to process Fragment. Configuration is
   * immutable and can be set only once.
   *
   * @return configuration of a Fragment.
   */
  public JsonObject getConfiguration() {
    return configuration.copy();
  }

  /**
   * Contains the body of a Fragment that is the final result of the fragment processing. Body can
   * be updated and transformed many times during processing.
   *
   * @return body of a Fragment.
   */
  public String getBody() {
    return body;
  }

  public Fragment setBody(String body) {
    this.body = body;
    return this;
  }

  /**
   * Any additional data that is associated with the Fragment. Payload can be appended (and
   * replaced) during processing but never cleared.
   *
   * @return additional data that is associated with the Fragment.
   */
  public JsonObject getPayload() {
    return payload.copy();
  }

  /**
   * Appends new entry int the Fragment's payload. Notice, that it may overwrite any existing info
   * in the payload, if the keys are identical.
   *
   * @param key - a key under which payload info will be saved.
   * @param value - a value of the payload info.
   * @return a reference to this, so the API can be used fluently.
   */
  public Fragment appendPayload(String key, Object value) {
    this.payload.put(key, value);
    return this;
  }

  /**
   * Merges given {@code JsonObject} with the existing payload. Notice, that it may overwrite any
   * existing info in the payload, if keys are identical.
   *
   * @param json a JsonObject to merge into payload.
   * @return a reference to this, so the API can be used fluently.
   */
  public Fragment mergeInPayload(JsonObject json) {
    this.payload.mergeIn(json);
    return this;
  }

  /**
   * Removes all the entries in Payload (JSON object).
   *
   * @return a reference to this, so the API can be used fluently.
   */
  public Fragment clearPayload() {
    this.payload.clear();
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Fragment fragment = (Fragment) o;
    return Objects.equals(id, fragment.id) &&
        Objects.equals(type, fragment.type) &&
        Objects.equals(configuration, fragment.configuration) &&
        Objects.equals(body, fragment.body) &&
        Objects.equals(payload, fragment.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, configuration, body, payload);
  }

  @Override
  public String toString() {
    return "Fragment{" +
        "id='" + id + '\'' +
        ", type='" + type + '\'' +
        ", configuration=" + configuration +
        ", body='" + body + '\'' +
        ", payload=" + payload +
        '}';
  }

  /**
   * Works similarly to {@code toString()} method, however Fragment's info is abbreviated. {@code
   * body} and {@code payload} are represented only by the first 256 characters and all newlines are
   * removed.
   *
   * @return abbreviated info about the Fragment.
   */
  public String abbreviate() {
    return "Fragment{" +
        "id='" + id + '\'' +
        ", type='" + type + '\'' +
        ", configuration=" + configuration +
        ", body='" + abbreviate(body) + '\'' +
        ", payload=" + abbreviate(payload.toString()) +
        '}';
  }

  private String abbreviate(String content) {
    return StringUtils.abbreviate(content.replaceAll("[\n\r\t]", ""),
        DEBUG_MAX_FRAGMENT_CONTENT_LOG_LENGTH);
  }

}
