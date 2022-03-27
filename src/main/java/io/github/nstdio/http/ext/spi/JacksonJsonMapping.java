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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class JacksonJsonMapping implements JsonMapping {
  private final ObjectMapper mapper;

  @SuppressWarnings("WeakerAccess")
  public JacksonJsonMapping(ObjectMapper objectMapper) {
    this.mapper = objectMapper;
  }

  JacksonJsonMapping() {
    this(new ObjectMapper());
  }

  @Override
  public <T> T read(InputStream in, Class<T> targetType) throws IOException {
    try (var stream = in) {
      return mapper.readValue(stream, targetType);
    }
  }

  @Override
  public <T> T read(byte[] bytes, Class<T> targetType) throws IOException {
    return mapper.readValue(bytes, targetType);
  }
}
