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
package io.github.nstdio.http.ext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

class Compression {

  static byte[] gzip(String in) {
    return compress(in, out -> {
      try {
        return new GZIPOutputStream(out, true);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  static byte[] deflate(String in) {
    return compress(in, out -> new DeflaterOutputStream(out, true));
  }

  private static byte[] compress(String in, Function<OutputStream, DeflaterOutputStream> compressorCreator) {
    try (var out = new ByteArrayOutputStream(); var compressor = compressorCreator.apply(out)) {
      compressor.write(in.getBytes(StandardCharsets.UTF_8));
      compressor.finish();
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
