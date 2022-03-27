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

import org.brotli.dec.BrotliInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class BrotliOrgCompressionFactory implements CompressionFactory {
  private final List<String> supported = List.of("br");

  @Override
  public List<String> supported() {
    return supported;
  }

  @Override
  public InputStream decompressing(InputStream in, String type) throws IOException {
    return new BrotliInputStream(in);
  }
}
