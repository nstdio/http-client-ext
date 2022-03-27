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

import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

interface JsonMappingContract {
  JsonMapping get();

  @Test
  default void shouldThrowIOExceptionWhenParsingException() {
    //given
    var bytes = "{".getBytes(StandardCharsets.UTF_8);
    var mapping = get();

    //when
    assertThrows(IOException.class, () -> mapping.read(bytes, Object.class));
  }

  @Test
  default void shouldThrowIOExceptionWhenParsingExceptionWithComplexType() {
    //given
    var bytes = new ByteArrayInputStream("{".getBytes(StandardCharsets.UTF_8));
    var mapping = get();
    var targetType = new TypeToken<List<String>>() {}.getType();

    //when
    assertThrows(IOException.class, () -> mapping.read(bytes, targetType));
  }

  @Test
  default void shouldCloseInputStream() throws IOException {
    //given
    byte[] jsonBytes = "{}".getBytes(StandardCharsets.UTF_8);
    var inSpy = spy(new ByteArrayInputStream(jsonBytes));
    var mapping = get();

    //when
    Object read = mapping.read(inSpy, Object.class);

    //then
    assertThat(read).isNotNull();
    verify(inSpy, atLeastOnce()).close();
  }

  @Test
  default void shouldReadFromByteArray() throws IOException {
    //given
    var jsonBytes = "{}".getBytes(StandardCharsets.UTF_8);
    var mapping = get();

    //when
    Object read = mapping.read(jsonBytes, Object.class);

    //then
    assertThat(read).isNotNull();
  }

  @Test
  default void shouldReadFromByteArrayUsingComplexType() throws IOException {
    //given
    var jsonBytes = "{\"a\": 1, \"b\": 2}".getBytes(StandardCharsets.UTF_8);
    var mapping = get();
    var targetType = new TypeToken<Map<String, Integer>>() {
    }.getType();

    //when
    Map<String, Integer> read = mapping.read(jsonBytes, targetType);

    //then
    assertThat(read)
        .hasSize(2)
        .containsEntry("a", 1)
        .containsEntry("b", 2);
  }

  @Test
  default void shouldReadFromInputStreamUsingComplexType() throws IOException {
    //given
    var jsonBytes = new ByteArrayInputStream("{\"a\": 1, \"b\": 2}".getBytes(StandardCharsets.UTF_8));
    var mapping = get();
    var targetType = new TypeToken<Map<String, Integer>>() {
    }.getType();

    //when
    Map<String, Integer> read = mapping.read(jsonBytes, targetType);

    //then
    assertThat(read)
        .hasSize(2)
        .containsEntry("a", 1)
        .containsEntry("b", 2);
  }
}
