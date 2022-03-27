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

import static io.github.nstdio.http.ext.spi.Classpath.isBrotli4jPresent;
import static io.github.nstdio.http.ext.spi.Classpath.isOrgBrotliPresent;

public class OptionalBrotliCompressionFactory extends CompressionFactoryBase {

  private final CompressionFactory delegate;

  public OptionalBrotliCompressionFactory() {
    if (isOrgBrotliPresent()) {
      delegate = new BrotliOrgCompressionFactory();
    } else if (isBrotli4jPresent()) {
      delegate = new Brotli4JCompressionFactory();
    } else {
      delegate = null;
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
