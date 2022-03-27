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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

import static io.github.nstdio.http.ext.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsonMetadataSerializerTest {
  @TempDir
  private Path dir;

  @Test
  void shouldReturnNullWhenCannotRead() {
    //given
    var file = dir.resolve("abc");
    var ser = new JsonMetadataSerializer();

    //when
    CacheEntryMetadata metadata = ser.read(file);

    //then
    assertNull(metadata);
  }

  @Test
  void shouldWriteAndRead() {
    //given
    var file = dir.resolve("abc");
    var responseInfo = ImmutableResponseInfo.builder()
        .headers(new HttpHeadersBuilder()
            .add("test", "1")
            .add("test", "2")
            .build())
        .statusCode(200)
        .version(HttpClient.Version.HTTP_1_1)
        .build();
    var request = HttpRequest.newBuilder(URI.create("https://example.com"))
        .header("abc", "1")
        .header("abc", "2")
        .header("abcd", "11")
        .header("abcd", "22")
        .version(HttpClient.Version.HTTP_2)
        .timeout(Duration.ofSeconds(30))
        .build();

    var metadata = new CacheEntryMetadata(10, 15, responseInfo, request, Clock.systemUTC());
    var ser = new JsonMetadataSerializer();

    //when
    ser.write(metadata, file);
    var actual = ser.read(file);

    //then
    assertThat(actual.requestTime()).isEqualTo(metadata.requestTime());
    assertThat(actual.responseTime()).isEqualTo(metadata.responseTime());
    assertThat(actual.request()).isEqualTo(metadata.request());

    assertThat(actual.response().statusCode()).isEqualTo(metadata.response().statusCode());
    assertThat(actual.response().version()).isEqualTo(metadata.response().version());
    assertThat(actual.response().headers()).isEqualTo(metadata.response().headers());
  }
}