package com.github.nstdio.http.ext;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

class DecompressingBodyHandlerIntegrationTest {

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();
  private final URI baseUri = URI.create("https://httpbin.org/");

  @Test
  void shouldCreate() throws Exception {
    //given
    var request = HttpRequest.newBuilder(baseUri.resolve("gzip"))
        .build();

    var body = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    var body2 = httpClient.send(request, BodyHandlers.ofDecompressing()).body();
  }
}
