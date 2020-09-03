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
package io.knotx.fragments.action.library.cache;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

@DataObject(generateConverter = true)
public class CacheActionOptions {

  private String payloadKey;
  private String cacheKey;
  private String logLevel;
  private String type;
  private JsonObject cache = new JsonObject();

  public CacheActionOptions() {
  }

  public CacheActionOptions(JsonObject json) {
    CacheActionOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject output = new JsonObject();
    CacheActionOptionsConverter.toJson(this, output);
    return output;
  }

  public String getPayloadKey() {
    return payloadKey;
  }

  public CacheActionOptions setPayloadKey(String payloadKey) {
    this.payloadKey = payloadKey;
    return this;
  }

  public String getCacheKey() {
    return cacheKey;
  }

  public CacheActionOptions setCacheKey(String cacheKey) {
    this.cacheKey = cacheKey;
    return this;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public CacheActionOptions setLogLevel(String logLevel) {
    this.logLevel = logLevel;
    return this;
  }

  public String getType() {
    return type;
  }

  public CacheActionOptions setType(String type) {
    this.type = type;
    return this;
  }

  public JsonObject getCache() {
    return cache;
  }

  public CacheActionOptions setCache(JsonObject cache) {
    this.cache = cache;
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
    CacheActionOptions that = (CacheActionOptions) o;
    return Objects.equals(payloadKey, that.payloadKey) &&
        Objects.equals(cacheKey, that.cacheKey) &&
        Objects.equals(logLevel, that.logLevel) &&
        Objects.equals(type, that.type) &&
        Objects.equals(cache, that.cache);
  }

  @Override
  public int hashCode() {
    return Objects.hash(payloadKey, cacheKey, logLevel, type, cache);
  }

  @Override
  public String toString() {
    return "CacheActionOptions{" +
        "payloadKey='" + payloadKey + '\'' +
        ", cacheKeySchema='" + cacheKey + '\'' +
        ", logLevel='" + logLevel + '\'' +
        ", type='" + type + '\'' +
        ", cache=" + cache +
        '}';
  }
}
