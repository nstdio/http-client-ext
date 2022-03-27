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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.nstdio.http.ext.Assertions.assertThat;
import static java.net.http.HttpResponse.BodyHandlers.ofInputStream;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DecompressingSubscriberIntegrationTest {
  private final HttpClient client = HttpClient.newHttpClient();

  @RegisterExtension
  WireMockExtension wm = WireMockExtension.newInstance()
      .configureStaticDsl(true)
      .failOnUnmatchedRequests(true)
      .options(wireMockConfig().dynamicPort())
      .build();

  @RepeatedTest(32)
  void shouldDecompressLargeBodyWithStringHandler() throws Exception {
    //given
    var data = setupLargeBodyDecompressionTest();
    HttpRequest request = data.request;
    String expectedBody = data.expectedBody;

    //when
    var stringResponse = client.send(request, info -> new DecompressingSubscriber<>(ofString().apply(info)));

    //then
    assertEquals(expectedBody.length(), stringResponse.body().length());
    assertThat(stringResponse).hasBody(expectedBody);
  }

  @RepeatedTest(1)
  @Disabled("Having problems with InputStream")
  void shouldHandleLargeBodyWithInputStream() throws Exception {
    //given
    var data = setupLargeBodyDecompressionTest();
    HttpRequest request = data.request;
    String expectedBody = data.expectedBody;

    //when
    var response = client.send(request, info -> new DecompressingSubscriber<>(ofInputStream().apply(info)));
    var body = IOUtils.toString(response.body(), UTF_8);

    assertEquals(expectedBody.length(), body.length());
    assertEquals(expectedBody, body);
  }

  private LargeBodyDataDecompression setupLargeBodyDecompressionTest() {
    return setupLargeBodyDecompressionTest(RandomStringUtils.randomAlphabetic(16384 * 10));
  }

  private LargeBodyDataDecompression setupLargeBodyDecompressionTest(String body) {
    byte[] gzippedBody = Compression.gzip(body);
    String testUrl = "/gzip";
    stubFor(get(urlEqualTo(testUrl))
        .willReturn(ok().withBody(gzippedBody)));
    var uri = URI.create(wm.getRuntimeInfo().getHttpBaseUrl()).resolve(testUrl);

    HttpRequest request = HttpRequest.newBuilder(uri).build();
    return new LargeBodyDataDecompression(request, body);
  }

  static class LargeBodyDataDecompression {
    private final HttpRequest request;
    private final String expectedBody;

    LargeBodyDataDecompression(HttpRequest request, String expectedBody) {
      this.request = request;
      this.expectedBody = expectedBody;
    }
  }
}