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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.nstdio.http.ext.Assertions.assertThat;
import static io.github.nstdio.http.ext.Assertions.awaitFor;
import static io.github.nstdio.http.ext.Headers.HEADER_CONTENT_ENCODING;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

class ExtendedHttpClientIntegrationTest {
  @RegisterExtension
  WireMockExtension wm = WireMockExtension.newInstance()
      .configureStaticDsl(true)
      .failOnUnmatchedRequests(true)
      .options(wireMockConfig().dynamicPort())
      .build();

  private URI resolve(String path) {
    return URI.create(wm.getRuntimeInfo().getHttpBaseUrl()).resolve(path);
  }

  @Nested
  class TransparentDecompressionTest {
    @Test
    void shouldTransparentlyDecompressAndCache() throws Exception {
      //given
      Cache cache = Cache.newInMemoryCacheBuilder().build();
      HttpClient client = ExtendedHttpClient.newBuilder()
          .cache(cache)
          .transparentEncoding(true)
          .build();
      var expectedBody = RandomStringUtils.randomAlphabetic(16);
      String testUrl = "/gzip";
      stubFor(get(urlEqualTo(testUrl))
          .withHeader("Accept-Encoding", equalTo("gzip,deflate"))
          .willReturn(ok()
              .withHeader("Cache-Control", "max-age=86000")
              .withBody(expectedBody))
      );
      HttpRequest request1 = HttpRequest.newBuilder(resolve(testUrl))
          .build();
      HttpRequest request2 = HttpRequest.newBuilder(resolve(testUrl))
          .build();

      //when + then
      var r1 = client.send(request1, ofString());
      assertThat(r1)
          .isNetwork()
          .hasBody(expectedBody)
          .hasNoHeader(HEADER_CONTENT_ENCODING);

      awaitFor(() -> {
        var r2 = client.send(request2, ofString());

        assertThat(r2)
            .isCached()
            .hasBody(expectedBody)
            .hasNoHeader(HEADER_CONTENT_ENCODING);
      });

    }

    @Test
    void shouldTransparentlyDecompress() throws Exception {
      //given
      HttpClient client = ExtendedHttpClient.newBuilder()
          .cache(Cache.noop())
          .transparentEncoding(true)
          .build();
      var expectedBody = RandomStringUtils.randomAlphabetic(16);
      String testUrl = "/gzip";
      stubFor(get(urlEqualTo(testUrl))
          .withHeader("Accept-Encoding", equalTo("gzip,deflate"))
          .willReturn(ok().withBody(expectedBody))
      );
      HttpRequest request = HttpRequest.newBuilder(resolve(testUrl)).build();

      //when
      var r1 = client.send(request, ofString());
      var r2 = client.sendAsync(request, ofString()).join();

      //then
      assertThat(r1)
          .isNetwork()
          .hasBody(expectedBody)
          .hasNoHeader(HEADER_CONTENT_ENCODING);

      assertThat(r2)
          .isNetwork()
          .hasBody(expectedBody)
          .hasNoHeader(HEADER_CONTENT_ENCODING);
    }
  }
}