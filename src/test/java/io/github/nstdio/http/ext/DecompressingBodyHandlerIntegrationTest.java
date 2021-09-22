/*
 * Copyright (C) 2021 Edgar Asatryan
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.nstdio.http.ext;

import static io.github.nstdio.http.ext.ext.BodyHandlers.ofDecompressing;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

class DecompressingBodyHandlerIntegrationTest {

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
    var json = IOUtils.toString(body);

    //then
    assertThat(json, isJson());
  }
}
