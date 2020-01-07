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
package io.knotx.fragments.handler.consumer;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class SnippetDebugFactory implements FragmentEventsConsumerFactory {

  private final String debugCss;
  private final String debugJs;

  public SnippetDebugFactory() {
    this.debugCss = loadResourceToString("debug/debug.css");
    this.debugJs = loadResourceToString("debug/debug.js");
  }

  @Override
  public String getName() {
    return "snippetDebug";
  }

  @Override
  public FragmentEventsConsumer create() {
    return new SnippetDebug(debugCss, debugJs);
  }

  private String loadResourceToString(String path) {
    ClassLoader classLoader = getClass().getClassLoader();
    try (InputStream is = classLoader.getResourceAsStream(path)) {
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(format("Failed to load file %s!", path), e);
    }
  }
}
