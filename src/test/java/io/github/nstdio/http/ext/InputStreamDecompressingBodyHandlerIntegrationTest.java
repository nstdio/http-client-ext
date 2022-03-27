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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static io.github.nstdio.http.ext.BodyHandlers.ofDecompressing;
import static org.hamcrest.MatcherAssert.assertThat;

class InputStreamDecompressingBodyHandlerIntegrationTest {

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final URI baseUri = URI.create("https://httpbin.org/");

  @ParameterizedTest
  @ValueSource(strings = {"gzip", "deflate"})
  void shouldCreate(String compression) throws Exception {
    //given
    var request = HttpRequest.newBuilder(baseUri.resolve(compression))
        .build();

    //when
    var body = httpClient.send(request, ofDecompressing()).body();
    var json = IOUtils.toString(body, StandardCharsets.UTF_8);

    //then
    assertThat(json, isJson());
  }
}
