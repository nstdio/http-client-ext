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
import java.util.List;

public class OptionalBrotliCompressionFactory extends CompressionFactoryBase {
  private static final boolean isBrotli4JPresent = isPresent("com.aayushatharva.brotli4j.Brotli4jLoader");
  private static final boolean isOrgBrotliPresent = isPresent("org.brotli.dec.BrotliInputStream");

  private final CompressionFactory delegate;

  public OptionalBrotliCompressionFactory() {
    if (isOrgBrotliPresent) {
      delegate = new BrotliOrgCompressionFactory();
    } else if (isBrotli4JPresent) {
      delegate = new Brotli4JCompressionFactory();
    } else {
      delegate = null;
    }
  }

  private static boolean isPresent(String cls) {
    try {
      Class.forName(cls);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public List<String> supported() {
    return delegate != null ? delegate.supported() : List.of();
  }

  @Override
  InputStream doDecompressing(InputStream in, String type) throws IOException {
    return delegate.decompressing(in, type);
  }
}
