package io.github.nstdio.http.ext;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Clock;

class ExtendedHttpClientTest {
    private ExtendedHttpClient client;
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        mockHttpClient = Mockito.mock(HttpClient.class);
        client = new ExtendedHttpClient(mockHttpClient, NullCache.INSTANCE, Clock.systemUTC());
    }

    @ParameterizedTest
    @ValueSource(classes = {
            IOException.class,
            InterruptedException.class,
            IllegalStateException.class,
            RuntimeException.class,
            OutOfMemoryError.class,
            SocketTimeoutException.class
    })
    void shouldPropagateExceptions(Class<Throwable> th) throws Exception {
        //given
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://example.com")).build();
        given(mockHttpClient.send(Mockito.any(), Mockito.any())).willThrow(th);

        //when + then
        Assertions.assertThatExceptionOfType(th)
                .isThrownBy(() -> client.send(request, ofString()));
        assertThrows(th, () -> client.send(request, ofString()));
    }
}