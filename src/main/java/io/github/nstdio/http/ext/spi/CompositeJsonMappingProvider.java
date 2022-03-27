/*
 * Copyright (C) 2022 Edgar Asatryan
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

package io.github.nstdio.http.ext.spi;

import static io.github.nstdio.http.ext.spi.Classpath.isGsonPresent;
import static io.github.nstdio.http.ext.spi.Classpath.isJacksonPresent;

class CompositeJsonMappingProvider extends JsonMappingProvider {
  private static final String NO_JSON_MAPPING_FOUND = "No JsonMapping implementation found. Please consider to add any of dependencies to classpath: 'com.fasterxml.jackson.core:jackson-databind', 'com.google.code.gson:gson'";

  static boolean hasAnyImplementation() {
    return isJacksonPresent() || isGsonPresent();
  }

  @Override
  public JsonMapping get() {
    if (isJacksonPresent()) {
      return JacksonMappingHolder.INSTANCE;
    }
    if (isGsonPresent()) {
      return GsonMappingHolder.INSTANCE;
    }

    throw new JsonMappingProviderNotFoundException(NO_JSON_MAPPING_FOUND);
  }

  private static class JacksonMappingHolder {
    private static final JacksonJsonMapping INSTANCE = new JacksonJsonMapping();
  }

  private static class GsonMappingHolder {
    private static final GsonJsonMapping INSTANCE = new GsonJsonMapping();
  }
}
