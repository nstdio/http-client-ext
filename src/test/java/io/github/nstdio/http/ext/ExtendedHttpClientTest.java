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