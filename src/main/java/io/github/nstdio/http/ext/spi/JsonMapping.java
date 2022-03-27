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

import java.io.IOException;
import java.io.InputStream;

/**
 * The simple strategy for binding JSON to Java objects.
 */
public interface JsonMapping {
  /**
   * Reads JSON data from the {@code in} and creates mapped object of type {@code targetType}. Note that {@code in}
   * might not be closed by the underlying implementation and caller should try to close {@code in}.
   *
   * @param in         The input source.
   * @param targetType The required type.
   * @param <T>        The type of object to create.
   *
   * @return The object created from JSON.
   *
   * @throws IOException When there is a JSON parsing or binding error or I/O error occurred.
   */
  <T> T read(InputStream in, Class<T> targetType) throws IOException;

  /**
   * Reads JSON data from the {@code bytes} and creates mapped object of type {@code targetType}.
   *
   * @param bytes      The input source.
   * @param targetType The required type.
   * @param <T>        The type of object to create.
   *
   * @return The object created from JSON.
   *
   * @throws IOException When there is a JSON parsing or binding error or I/O error occurred.
   */
  <T> T read(byte[] bytes, Class<T> targetType) throws IOException;
}
