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
package io.knotx.fragments.action.cache;

import io.knotx.fragments.action.api.log.ActionLogLevel;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import java.util.Objects;

@DataObject(generateConverter = true)
public class CacheOptions {

  private String payloadKey;
  private String cacheKey;
  private JsonObject cache = new JsonObject();
  private String logLevel = ActionLogLevel.ERROR.name();

  public CacheOptions() {
  }

  public CacheOptions(JsonObject json) {
    CacheOptionsConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CacheOptionsConverter.toJson(this, json);
    return json;
  }

  /**
   * @return Fragment payload's key used to store the cached or computed value
   */
  public String getPayloadKey() {
    return payloadKey;
  }

  /**
   * Sets the Fragment payload's key under which the cached data is to be placed. In case of a cache
   * miss, the value to be cached will be fetched from the computed Fragment's playload using this
   * key.
   *
   * @param payloadKey Fragment payload's key to cache
   * @return a reference to this, so the API can be used fluently
   */
  public CacheOptions setPayloadKey(String payloadKey) {
    this.payloadKey = payloadKey;
    return this;
  }

  /**
   * @return cache key schema that will be filled with FragmentContext's placeholders
   */
  public String getCacheKey() {
    return cacheKey;
  }

  /**
   * Sets the cache key schema, possibly containing <a href="https://github.com/Knotx/knotx-server-http/tree/master/common/placeholders">Knot.x
   * Server Common Placeholders</a>. The placeholders will be evaluated using FragmentContext's data
   * and used to generate the actual key. With this key the cache will be polled. In case of a cache
   * miss, it will be used to store the computed value (when computation succeeds).
   *
   * @param cacheKey cache key schema that will be filled with FragmentContext's placeholders
   * @return a reference to this, so the API can be used fluently
   */
  public CacheOptions setCacheKey(String cacheKey) {
    this.cacheKey = cacheKey;
    return this;
  }

  /**
   * @return implementation-specific options for the underlying cache
   */
  public JsonObject getCache() {
    return cache;
  }

  /**
   * Sets the implementation-specific options for the underlying cache
   *
   * @param cache implementation-specific options for the underlying cache
   * @return a reference to this, so the API can be used fluently
   */
  public CacheOptions setCache(JsonObject cache) {
    this.cache = cache;
    return this;
  }

  public String getLogLevel() {
    return logLevel;
  }

  /**
   * Set level of action logs.
   *
   * @param logLevel level of action logs
   * @return a reference to this, so the API can be used fluently
   */
  public CacheOptions setLogLevel(String logLevel) {
    this.logLevel = logLevel;
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
    CacheOptions that = (CacheOptions) o;
    return Objects.equals(payloadKey, that.payloadKey) &&
        Objects.equals(cacheKey, that.cacheKey) &&
        Objects.equals(cache, that.cache) &&
        Objects.equals(logLevel, that.logLevel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(payloadKey, cacheKey, cache, logLevel);
  }

  @Override
  public String toString() {
    return "CacheOptions{" +
        "payloadKey='" + payloadKey + '\'' +
        ", cacheKey='" + cacheKey + '\'' +
        ", cache=" + cache +
        ", logLevel='" + logLevel + '\'' +
        '}';
  }
}
