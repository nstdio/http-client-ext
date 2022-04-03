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
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * The strategy for creating I/O streams.
 */
interface StreamFactory {
  /**
   * Creates a new {@code OutputStream} for {@code path}.
   *
   * @param path    The path to the file.
   * @param options The options.
   *
   * @return a new output stream.
   */
  OutputStream output(Path path, OpenOption... options) throws IOException;

  /**
   * Creates a new {@code WritableByteChannel} for {@code path}.
   *
   * @param path    The path to the file.
   * @param options The options.
   *
   * @return a new writable channel.
   */
  default WritableByteChannel writable(Path path, OpenOption... options) throws IOException {
    return Channels.newChannel(output(path, options));
  }

  /**
   * Creates a new {@code InputStream} for {@code path}.
   *
   * @param path    The path to the file.
   * @param options The options.
   *
   * @return a new input stream.
   */
  InputStream input(Path path, OpenOption... options) throws IOException;

  /**
   * Creates a new {@code ReadableByteChannel} for {@code path}.
   *
   * @param path    The path to the file.
   * @param options The options.
   *
   * @return a new readable channel.
   */
  default ReadableByteChannel readable(Path path, OpenOption... options) throws IOException {
    return Channels.newChannel(input(path, options));
  }
}
