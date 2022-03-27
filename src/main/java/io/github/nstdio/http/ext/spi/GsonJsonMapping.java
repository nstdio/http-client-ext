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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GsonJsonMapping implements JsonMapping {
  private final Gson gson;

  @SuppressWarnings("WeakerAccess")
  public GsonJsonMapping(Gson gson) {
    this.gson = Objects.requireNonNull(gson);
  }

  GsonJsonMapping() {
    this(new Gson());
  }

  @Override
  public <T> T read(InputStream in, Class<T> targetType) throws IOException {
    try (var reader = new InputStreamReader(in, UTF_8)) {
      return gson.fromJson(reader, targetType);
    } catch (JsonParseException e) {
      throw new IOException(e);
    }
  }

  @Override
  public <T> T read(byte[] bytes, Class<T> targetType) throws IOException {
    return read(new ByteArrayInputStream(bytes), targetType);
  }
}
