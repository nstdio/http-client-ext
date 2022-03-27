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

import io.github.nstdio.http.ext.jupiter.EnabledIfOnClasspath;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static io.github.nstdio.http.ext.OptionalDependencies.GSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@EnabledIfOnClasspath(GSON)
class GsonJsonMappingTest {

  @Test
  void shouldThrowIOExceptionWhenParsingException() {
    //given
    var bytes = "{".getBytes(StandardCharsets.UTF_8);
    var mapping = new GsonJsonMapping();

    //when
    assertThatIOException()
        .isThrownBy(() -> mapping.read(bytes, Object.class))
        .havingCause();
  }

  @Test
  void shouldCloseInputStream() throws IOException {
    //given
    var inSpy = spy(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
    var mapping = new GsonJsonMapping();

    //when
    Object read = mapping.read(inSpy, Object.class);

    //then
    assertThat(read).isNotNull();
    verify(inSpy).close();
  }

  @Test
  void shouldReadFromByteArray() throws IOException {
    //given
    var bytes = "{}".getBytes(StandardCharsets.UTF_8);
    var mapping = new GsonJsonMapping();

    //when
    Object read = mapping.read(bytes, Object.class);

    //then
    assertThat(read).isNotNull();
  }
}