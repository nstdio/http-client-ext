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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.nio.file.StandardOpenOption.READ;

class SimpleStreamFactory implements StreamFactory {
  private static void assertNotContains(OpenOption[] options, StandardOpenOption needle) {
    for (OpenOption option : options) {
      if (option == needle) {
        throw new IllegalArgumentException(needle + " not allowed");
      }
    }
  }

  @Override
  public OutputStream output(Path path, OpenOption... options) throws IOException {
    return Files.newOutputStream(path, options);
  }

  @Override
  public WritableByteChannel writable(Path path, OpenOption... options) throws IOException {
    assertNotContains(options, READ);

    return Files.newByteChannel(path, options);
  }

  @Override
  public InputStream input(Path path, OpenOption... options) throws IOException {
    return Files.newInputStream(path, options);
  }
}
