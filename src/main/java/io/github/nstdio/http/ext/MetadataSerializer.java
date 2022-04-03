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

import java.nio.file.Path;

import static io.github.nstdio.http.ext.spi.Classpath.isGsonPresent;
import static io.github.nstdio.http.ext.spi.Classpath.isJacksonPresent;

interface MetadataSerializer {
  static void requireAvailability() {
    if (!isJacksonPresent() && !isGsonPresent()) {
      throw new IllegalStateException("In order to use disk cache please add either Jackson or Gson to your dependencies");
    }
  }

  static MetadataSerializer findAvailable(StreamFactory streamFactory) {
    if (isJacksonPresent()) {
      return new JacksonMetadataSerializer(streamFactory);
    }

    if (isGsonPresent()) {
      return new GsonMetadataSerializer(streamFactory);
    }

    return null;
  }

  void write(CacheEntryMetadata metadata, Path path);

  CacheEntryMetadata read(Path path);
}
