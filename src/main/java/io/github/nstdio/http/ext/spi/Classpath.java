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

public class Classpath {
  private static final boolean JACKSON = isPresent("com.fasterxml.jackson.databind.ObjectMapper");
  private static final boolean GSON = isPresent("com.google.gson.Gson");
  private static final boolean ORG_BROTLI = isPresent("org.brotli.dec.BrotliInputStream");
  private static final boolean BROTLI_4J = isPresent("com.aayushatharva.brotli4j.Brotli4jLoader");

  private Classpath() {
  }

  public static boolean isJacksonPresent() {
    return JACKSON;
  }

  public static boolean isGsonPresent() {
    return GSON;
  }

  public static boolean isOrgBrotliPresent() {
    return ORG_BROTLI;
  }

  public static boolean isBrotli4jPresent() {
    return BROTLI_4J;
  }

  public static boolean isPresent(String cls) {
    try {
      Class.forName(cls);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
