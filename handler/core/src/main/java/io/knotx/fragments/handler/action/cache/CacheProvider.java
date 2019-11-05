package io.knotx.fragments.handler.action.cache;

import io.reactivex.Maybe;

public interface CacheProvider {

  Maybe<Object> get(String key);

  void put(String key, Object value);

}
