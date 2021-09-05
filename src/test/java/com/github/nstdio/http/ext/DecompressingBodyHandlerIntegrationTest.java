package com.github.nstdio.http.ext;

import static com.github.nstdio.http.ext.BodyHandlers.ofDecompressing;
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
