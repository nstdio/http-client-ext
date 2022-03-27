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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

class DecompressingSubscriberSpiTest {
  private final HttpClient client = HttpClient.newHttpClient();

  @ParameterizedTest
  @ValueSource(strings = {"brotli", "gzip", "deflate"})
  void shouldDecompress(String in) throws IOException, InterruptedException {
    //given
    URI uri = URI.create("https://httpbin.org/").resolve(in);

    //when
    HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).build(), BodyHandlers.ofDecompressing(ofString()));
    String body = response.body();

    //then
    isJson().matches(body);
  }
}
