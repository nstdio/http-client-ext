/*
 * Copyright (C) 2022-2025 the original author or authors.
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

public class Classpath {

  private Classpath() {
  }

  public static boolean isJacksonPresent() {
    return isPresent("com.fasterxml.jackson.databind.ObjectMapper");
  }

  public static boolean isGsonPresent() {
    return isPresent("com.google.gson.Gson");
  }

  public static boolean isOrgBrotliPresent() {
    return isPresent("org.brotli.dec.BrotliInputStream");
  }

  public static boolean isBrotli4jPresent() {
    return isPresent("com.aayushatharva.brotli4j.Brotli4jLoader");
  }

  public static boolean isZstdJniPresent() {
    return isPresent("com.github.luben.zstd.ZstdInputStream");
  }

  public static boolean isPresent(String cls) {
    try {
      Class.forName(cls, false, Thread.currentThread().getContextClassLoader());
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
