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
package io.knotx.fragments.action.library.cache.operations;

import static io.knotx.fragments.action.library.cache.TestUtils.CACHE_KEY;
import static io.knotx.fragments.action.library.cache.TestUtils.SOME_VALUE;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.knotx.fragments.action.api.log.ActionLogger;
import io.knotx.fragments.api.Fragment;
import io.knotx.fragments.api.FragmentResult;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CacheActionLoggerTest {

  @Mock
  private ActionLogger actionLogger;

  private CacheActionLogger tested;

  @BeforeEach
  void setUp() {
    tested = new CacheActionLogger(actionLogger);
  }

  @Test
  @DisplayName("Expect cache hit logged on INFO level")
  void cacheHit() {
    tested.onLookup(CACHE_KEY);
    tested.onHit(SOME_VALUE);

    verify(actionLogger, times(1)).info(any(), (JsonObject) any());
  }

  @Test
  @DisplayName("Expect cache miss logged on INFO level")
  void cacheMiss() {
    tested.onLookup(CACHE_KEY);
    tested.onMiss(SOME_VALUE);

    verify(actionLogger, times(1)).info(any(), (JsonObject) any());
  }

  @Test
  @DisplayName("Expect cache pass logged on ERROR level")
  void cachePass() {
    tested.onLookup(CACHE_KEY);
    tested.onPass();

    verify(actionLogger, times(1)).error(any(), any(JsonObject.class));
  }

  @Test
  @DisplayName("Expect error logged on ERROR level")
  void error() {
    tested.onLookup(CACHE_KEY);
    tested.onError(new RuntimeException());

    verify(actionLogger, times(1)).error(any(Throwable.class));
  }

  @Test
  @DisplayName("Expect success retrieval logged as success")
  void successRetrieval() {
    tested.onLookup(CACHE_KEY);
    tested.onRetrieveStart();
    tested.onRetrieveEnd(successResult());

    verify(actionLogger, times(1)).doActionLog(anyLong(), any());
  }

  @Test
  @DisplayName("Expect failed retrieval logged as failure")
  void failedRetrieval() {
    tested.onLookup(CACHE_KEY);
    tested.onRetrieveStart();
    tested.onRetrieveEnd(failedResult());

    verify(actionLogger, times(1)).failureDoActionLog(anyLong(), any());
  }

  @Test
  @DisplayName("Expect calculated retrieval time shorter that test's")
  void timeCalculation() throws InterruptedException {
    tested.onLookup(CACHE_KEY);

    long startTime = now().toEpochMilli();
    tested.onRetrieveStart();
    TimeUnit.MILLISECONDS.sleep(50);
    tested.onRetrieveEnd(successResult());
    long testDuration = now().toEpochMilli() - startTime;

    ArgumentCaptor<Long> duration = ArgumentCaptor.forClass(Long.class);
    verify(actionLogger, times(1)).doActionLog(duration.capture(), any());

    assertTrue(duration.getValue() <= testDuration);
  }

  private FragmentResult successResult() {
    return FragmentResult.success(new Fragment("", new JsonObject(), ""));
  }

  private FragmentResult failedResult() {
    return FragmentResult.fail(new Fragment("", new JsonObject(), ""), new RuntimeException());
  }

}
