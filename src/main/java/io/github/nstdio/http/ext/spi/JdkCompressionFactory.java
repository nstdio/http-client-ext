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
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class JdkCompressionFactory implements CompressionFactory {
  private final List<String> supportedTypes = List.of("gzip", "x-gzip", "deflate");

  @Override
  public List<String> supported() {
    return supportedTypes;
  }

  @Override
  public InputStream decompressing(InputStream in, String type) throws IOException {
    switch (type) {
      case "x-gzip":
      case "gzip":
        return new GZIPInputStream(in);
      case "deflate":
        return new InflaterInputStream(in);
      default:
        throw new IllegalArgumentException("Unsupported type:" + type);
    }
  }
}
