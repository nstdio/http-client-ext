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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.http.Fault.EMPTY_RESPONSE;
import static io.github.nstdio.http.ext.Assertions.assertThat;
import static io.github.nstdio.http.ext.Assertions.await;
import static io.github.nstdio.http.ext.Assertions.awaitFor;
import static io.github.nstdio.http.ext.Headers.HEADER_CACHE_CONTROL;
import static io.github.nstdio.http.ext.Headers.HEADER_DATE;
import static io.github.nstdio.http.ext.Headers.HEADER_EXPIRES;
import static io.github.nstdio.http.ext.Headers.HEADER_IF_MODIFIED_SINCE;
import static io.github.nstdio.http.ext.Headers.toRFC1123;
import static io.github.nstdio.http.ext.Matchers.isCached;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Clock;
import java.time.Duration;

class InMemoryExtendedHttpClientIntegrationTest implements ExtendedHttpClientContract {

    private final Clock defaultClock = Clock.systemUTC();
    private ExtendedHttpClient client;
    private String path;
    private Cache cache;

    @BeforeEach
    void setUp() {
        cache = Cache.newInMemoryCacheBuilder()
                .requestFilter(request -> true)
                .responseFilter(response -> true)
                .build();
        client = new ExtendedHttpClient(HttpClient.newHttpClient(), cache, defaultClock);
        path = "/resource";
    }

    @Override
    public String path() {
        return path;
    }

    @Test
    void shouldRespondWithCachedWhenNotModified() throws IOException, InterruptedException {
        //given
        var urlPattern = urlEqualTo(path);
        var clock = FixedRateTickClock.of(defaultClock, Duration.ofSeconds(2));
        client = new ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock);

        String date = toRFC1123(clock.instant().minusSeconds(2));
        stubFor(get(urlPattern)
                .willReturn(WireMock.ok()
                        .withHeader(HEADER_DATE, date)
                        .withHeader(HEADER_CACHE_CONTROL, "public,max-age=1")
                        .withBody("Hello world!")
                )
        );
        stubFor(get(urlPattern)
                .withHeader(HEADER_IF_MODIFIED_SINCE, equalTo(date))
                .willReturn(status(304).withHeader(HEADER_CACHE_CONTROL, "private, max-age=1"))
        );

        var request = requestBuilder().build();

        //when
        var r1 = send(request);
        var r2 = await().until(() -> send(request), isCached());
        var r3 = await().until(() -> send(request), isCached());

        //then
        assertThat(r1)
                .hasHeader(HEADER_CACHE_CONTROL, "public,max-age=1")
                .isNetwork();
        assertThat(r2)
                .hasHeader(HEADER_CACHE_CONTROL, "private, max-age=1")
                .isSemanticallyEqualTo(r3);

        verify(1, getRequestedFor(urlPattern).withoutHeader(HEADER_IF_MODIFIED_SINCE));
        verify(2, getRequestedFor(urlPattern).withHeader(HEADER_IF_MODIFIED_SINCE, equalTo(date)));
    }

    @Test
    void shouldRespectMaxStaleRequests() throws Exception {
        //given
        Duration tickDuration = Duration.ofSeconds(1);
        var clock = FixedRateTickClock.of(defaultClock, tickDuration);
        client = new ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock);
        String date = toRFC1123(clock.instant().minusMillis(tickDuration.toMillis()));

        stubFor(get(urlEqualTo(path()))
                .willReturn(WireMock.ok()
                        .withHeader(HEADER_DATE, date)
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=1")
                        .withBody("abc")
                )
        );

        //when
        var r1 = send(requestBuilder().build());
        var r2 = await().until(() -> send(requestBuilder().header(HEADER_CACHE_CONTROL, "max-stale=5").build()), isCached());
        var r3 = send(requestBuilder().header(HEADER_CACHE_CONTROL, "max-stale=4").build());

        //then
        assertThat(r1).isNetwork();
        assertThat(r2).isCached();
        assertThat(r3).isNetwork();
    }

    @Test
    void shouldUseResponseTimeWhenDateHeaderMissing() throws Exception {
        //given
        Duration tickDuration = Duration.ofSeconds(1);
        var clock = FixedRateTickClock.of(defaultClock, tickDuration);
        client = new ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock);
        String expires = toRFC1123(defaultClock.instant().plusSeconds(6));

        stubFor(get(urlEqualTo(path()))
                .willReturn(WireMock.ok()
                        .withHeader(HEADER_EXPIRES, expires)
                        .withBody("abc")
                )
        );
        var request = requestBuilder().build();

        //when
        var r1 = send(request);
        var r2 = await().until(() -> send(request), isCached());
        var r3 = await().until(() -> send(request), not(isCached()));

        //then
        assertThat(r1).isNetwork();
    }

    @Test
    @DisplayName("Should respond with 504 when server requires validation, but validation request fails.")
    void shouldReturn504WhenMustRevalidate() throws Exception {
        //given
        Duration tickDuration = Duration.ofSeconds(1);
        var clock = FixedRateTickClock.of(defaultClock, tickDuration);
        client = new ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock);
        String date = toRFC1123(clock.instant().minusMillis(tickDuration.toMillis()));
        var urlPattern = urlEqualTo(path());

        stubFor(get(urlPattern)
                .willReturn(WireMock.ok()
                        .withHeader(HEADER_DATE, date)
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=1,must-revalidate")
                        .withBody("abc")
                )
        );
        stubFor(get(urlPattern)
                .withHeader(HEADER_IF_MODIFIED_SINCE, equalTo(date))
                .willReturn(aResponse().withFault(EMPTY_RESPONSE))
        );
        var request = requestBuilder().build();

        //when + then
        var r1 = send(request);
        assertThat(r1).isNetwork().hasStatusCode(200).hasBody("abc");

        awaitFor(() -> {
            var r2 = send(request);
            assertThat(r2).isNotNetwork().isNotCached().hasStatusCode(504);
        });
    }

    @Test
    void shouldNotCacheWhenFiltered() throws Exception {
        //given
        URI cacheableUri = ExtendedHttpClientContract.resolve(path());
        URI notCacheableUri = cacheableUri.resolve("/no-cache");

        cache = Cache.newInMemoryCacheBuilder()
                .requestFilter(r -> r.uri().equals(cacheableUri))
                .build();
        client = new ExtendedHttpClient(HttpClient.newHttpClient(), cache, clock());

        stubFor(get(urlEqualTo(notCacheableUri.getPath()))
                .willReturn(WireMock.ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=512")
                        .withBody("abc")
                )
        );
        HttpRequest request = HttpRequest.newBuilder().uri(notCacheableUri).build();

        //when
        var r1 = send(request);
        var r2 = send(request);

        //then
        assertThat(r1).isNetwork();
        assertThat(r2).isNetwork();

        assertNull(cache.get(request));
    }

    @Override
    public ExtendedHttpClient client() {
        return client;
    }
}