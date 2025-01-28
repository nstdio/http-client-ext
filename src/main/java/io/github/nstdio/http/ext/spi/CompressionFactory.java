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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * The strategy to create on-the-fly decompressing {@code InputStream}.
 */
public interface CompressionFactory {

  /**
   * The list of supported decompression directives like:
   * <pre>
   *      List.of("gzip", "x-gzip")
   * </pre>
   *
   * @return Supported types. Never null.
   */
  List<String> supported();

  /**
   * Wraps the provided {@code InputStream} into stream capable of decompressing {@link #supported()} compression
   * algorithms.
   *
   * @param in   The input stream.
   * @param type One of {@link #supported()} types.
   *
   * @return Wrapped input stream.
   *
   * @throws IOException              When {@code in} does not represent a supported compression type or I/O error
   *                                  occures.
   * @throws IllegalArgumentException When type {@link #supported()} does not contain {@code type}.
   */
  InputStream decompressing(InputStream in, String type) throws IOException;
}
