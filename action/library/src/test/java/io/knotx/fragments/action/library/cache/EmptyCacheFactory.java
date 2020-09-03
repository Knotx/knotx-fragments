package io.knotx.fragments.action.library.cache;

import io.knotx.commons.cache.Cache;
import io.knotx.commons.cache.CacheFactory;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import java.util.function.Supplier;

public class EmptyCacheFactory implements CacheFactory {

  private static final Supplier<Cache> EMPTY_CACHE = () -> new Cache() {
    @Override
    public Maybe<Object> get(String key) {
      return Maybe.empty();
    }

    @Override
    public void put(String key, Object value) {

    }
  };

  @Override
  public String getType() {
    return "empty-cache";
  }

  @Override
  public Cache create(JsonObject config) {
    if (config.containsKey("invalidOption")) {
      throw new IllegalArgumentException();
    }
    return EMPTY_CACHE.get();
  }
}
