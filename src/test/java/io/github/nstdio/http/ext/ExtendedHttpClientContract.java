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

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.github.nstdio.http.ext.Assertions.assertThat;
import static io.github.nstdio.http.ext.Headers.HEADER_CACHE_CONTROL;
import static io.github.nstdio.http.ext.Headers.HEADER_DATE;
import static io.github.nstdio.http.ext.Headers.HEADER_ETAG;
import static io.github.nstdio.http.ext.Headers.HEADER_IF_NONE_MATCH;
import static io.github.nstdio.http.ext.Headers.HEADER_LAST_MODIFIED;
import static io.github.nstdio.http.ext.Headers.toRFC1123;
import static io.github.nstdio.http.ext.Matchers.isCached;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface ExtendedHttpClientContract {
    @RegisterExtension
    WireMockExtension wm = WireMockExtension.newInstance()
            .configureStaticDsl(true)
            .failOnUnmatchedRequests(true)
            .options(wireMockConfig().dynamicPort())
            .build();

    /**
     * The maximum time to wait until cache gets written.
     */
    // TODO: Read from arguments, properties, env
    Duration CACHE_WRITE_DELAY = Duration.ofMillis(500);
    Duration POLL_INTERVAL = Duration.ofMillis(5);

    static URI resolve(String path) {
        return URI.create(wm.getRuntimeInfo().getHttpBaseUrl()).resolve(path);
    }

    static void awaitFor(ThrowingRunnable r) {
        await().untilAsserted(r);
    }

    static ConditionFactory await() {
        return Awaitility.await().pollInterval(POLL_INTERVAL).atMost(CACHE_WRITE_DELAY);
    }

    /**
     * The client under the test.
     */
    ExtendedHttpClient client();

    default HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client().send(request, ofString());
    }

    /**
     * The clock to use when performing freshness calculations.
     */
    default Clock clock() {
        return Clock.systemUTC();
    }

    default String path() {
        return "/resource";
    }

    default HttpRequest.Builder requestBuilder() {
        return HttpRequest.newBuilder(resolve(path()));
    }

    @Test
    default void shouldSupportETagForCaching() throws Exception {
        //given
        var etag = "v1";
        stubFor(get(urlEqualTo(path()))
                .willReturn(ok()
                        .withHeader(HEADER_ETAG, etag)
                        .withBody("abc")
                )
        );
        stubFor(get(urlEqualTo(path()))
                .withHeader(HEADER_IF_NONE_MATCH, equalTo(etag))
                .willReturn(status(304))
        );

        //when + then
        var r1 = send(requestBuilder().build());
        assertThat(r1).isNotCached();

        awaitFor(() -> {
            var r2 = send(requestBuilder().build());
            assertThat(r2).isCached()
                    .hasStatusCode(200)
                    .hasBody("abc");
        });
    }

    @Test
    default void shouldApplyHeuristicFreshness() throws Exception {
        //given
        stubFor(get(urlEqualTo(path()))
                .willReturn(ok()
                        .withHeader(HEADER_LAST_MODIFIED, Headers.toRFC1123(Instant.now().minusSeconds(60)))
                        .withBody("abc")
                )
        );

        //when + then
        var r1 = send(requestBuilder().build());
        assertThat(r1).isNotCached();

        awaitFor(() -> {
            var r2 = send(requestBuilder().build());
            assertThat(r2).isCached();
        });
    }

    @Test
    default void shouldWorkWithOnlyIfCached() throws Exception {
        //given
        stubFor(get(urlEqualTo(path()))
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=1")
                        .withBody("abc")
                )
        );

        //when + then
        var r1 = send(requestBuilder().build());
        assertThat(r1).isNotCached().hasStatusCode(200);

        awaitFor(() -> {
            var r2 = send(requestBuilder().header(HEADER_CACHE_CONTROL, "only-if-cached").build());
            assertThat(r2).isCached();
        });

        awaitFor(() -> {
            var r3 = send(requestBuilder().header(HEADER_CACHE_CONTROL, "only-if-cached,max-age=2").build());
            assertThat(r3).isCached();

        });

        var r4 = send(requestBuilder().header(HEADER_CACHE_CONTROL, "only-if-cached,max-age=0").build());
        assertThat(r4)
                .isNotCached()
                .hasStatusCode(504);
    }

    @ParameterizedTest
    @ValueSource(ints = {
            201, 202, 205, 207, 226,
            302, 303, 304, 305, 306, 307, 308,
            400, 401, 402, 403, 406, 407, 408, 409, 411, 412, 413, 415, 416, 417, 418, 421, 422, 423, 424, 425, 426, 428, 429, 431, 451,
            500, 502, 503, 504, 505, 506, 507, 508, 510, 511
    })
    default void shouldNotCacheStatusCodesOtherThen(int statusCode) throws Exception {
        //given
        var body = "abc";
        stubFor(get(urlEqualTo(path()))
                .willReturn(status(statusCode)
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=360")
                        .withBody(body)
                )
        );

        //when
        var r1 = send(requestBuilder().build());
        var r2 = send(requestBuilder().build());

        //then
        Assertions.assertThat(List.of(r1, r2)).allSatisfy(r -> {
            assertThat(r).isNotCached().hasStatusCode(statusCode).hasBody(body);
        });
    }

    @Test
    default void shouldFailWhenOnlyIfCachedWithEmptyCache() throws Exception {
        //given
        var request = requestBuilder()
                .header(HEADER_CACHE_CONTROL, CacheControl.builder().onlyIfCached().build().toString())
                .build();

        //when
        var r1 = send(request);

        //then
        assertThat(r1).hasStatusCode(504);
        Assertions.assertThat(wm.getServeEvents().getRequests()).isEmpty();
    }

    @Test
    default void shouldRespectMinFreshRequests() throws Exception {
        //given
        stubFor(get(urlEqualTo(path()))
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=3")
                        .withBody("abc")
                )
        );

        //when + then
        var r1 = send(requestBuilder().build());
        assertThat(r1).isNotCached();

        awaitFor(() -> {
            var r2 = send(requestBuilder().header(HEADER_CACHE_CONTROL, "min-fresh=1").build());
            assertThat(r2).isCached();
        });

        MILLISECONDS.sleep(2050);
        var r3 = send(requestBuilder().header(HEADER_CACHE_CONTROL, "min-fresh=1").build());
        assertThat(r3).isNotCached();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "no-cache",
            "max-age=0"
    })
    default void shouldNotRespondWithCacheWhenNoCacheProvided(String cacheControl) throws IOException, InterruptedException {
        //given
        var urlPattern = urlEqualTo(path());
        stubFor(get(urlPattern)
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=84600")
                        .withHeader("Content-Type", "text/plain")
                        .withBody("abc")
                )
        );

        var count = 5;
        for (int i = 0; i < count; i++) {
            var request = requestBuilder()
                    .header(HEADER_CACHE_CONTROL, cacheControl)
                    .build();
            var noCacheControlRequest = requestBuilder().build();

            //when + then
            var r1 = send(request);
            assertThat(r1).isNotCached().hasBody("abc");

            awaitFor(() -> {
                var r2 = send(noCacheControlRequest);
                assertThat(r2).isCached().hasBody("abc");
            });
        }

        verify(count, getRequestedFor(urlPattern).withHeader(HEADER_CACHE_CONTROL, equalTo(cacheControl)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "no-store",
            "no-store, no-cache"
    })
    default void shouldNotCacheWhenNoStoreProvided(String cacheControl) throws IOException, InterruptedException {
        //given
        var urlPattern = urlEqualTo(path());
        stubFor(get(urlPattern)
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=84600")
                        .withHeader("Content-Type", "text/plain")
                        .withBody("abc")
                )
        );
        send(requestBuilder().build()); // make r2 cached

        var count = 5;
        for (int i = 0; i < count; i++) {
            var request = requestBuilder()
                    .header(HEADER_CACHE_CONTROL, cacheControl)
                    .build();

            //when + then
            var r1 = send(request);
            assertThat(r1).isNotCached().hasBody("abc");

            awaitFor(() -> {
                var r2 = send(requestBuilder().build());
                assertThat(r2).isCached().hasBody("abc");
            });
        }

        verify(count, getRequestedFor(urlPattern).withHeader(HEADER_CACHE_CONTROL, equalTo(cacheControl)));
        verify(1, getRequestedFor(urlPattern).withoutHeader(HEADER_CACHE_CONTROL));
    }

    @Test
    default void shouldCacheWhenHeadersDifferWithoutVary() throws IOException, InterruptedException {
        //given
        var cacheControlValue = "public,max-age=20";
        var urlPattern = urlEqualTo(path());
        stubFor(get(urlPattern)
                .withHeader("Accept", equalTo("text/plain"))
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, cacheControlValue)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello world!")
                )
        );

        //when + then
        var r1 = send(requestBuilder().header("Accept", "text/plain").build());
        assertThat(r1)
                .isNotCached()
                .hasBody("Hello world!");

        awaitFor(() -> {
            var r2 = send(requestBuilder().header("Accept", "application/json").build());
            assertThat(r2)
                    .isCached()
                    .hasBody("Hello world!");
        });

        verify(1, getRequestedFor(urlPattern).withHeader("Accept", equalTo("text/plain")));
    }

    @Test
    default void shouldNotCacheWithVaryAsterisk() throws Exception {
        //given
        var cacheControlValue = "public,max-age=20";
        var count = 9;
        var urlPattern = urlEqualTo(path());
        stubFor(get(urlPattern)
                .withHeader("Accept", equalTo("text/plain"))
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, cacheControlValue)
                        .withHeader("Content-Type", "text/plain")
                        .withHeader("Vary", "*")
                        .withBody("Hello world!")
                )
        );
        var request = requestBuilder()
                .header("Accept", "text/plain")
                .build();

        //when + then
        for (int i = 0; i < count; i++) {
            var r1 = send(request);
            assertThat(r1)
                    .isNotCached()
                    .hasBody("Hello world!");
        }

        verify(count, getRequestedFor(urlPattern).withHeader("Accept", equalTo("text/plain")));
    }

    @Test
    default void shouldCacheWithVary() throws Exception {
        //given
        var cacheControlValue = "public,max-age=20";
        var count = 9;
        var varyValues = new String[]{"Accept", "Accept-Encoding", "User-Agent"};

        var textBody = "Hello world!";
        var jsonBody = "\"Hello world!\"";
        var urlPattern = urlEqualTo(path());
        stubFor(get(urlPattern)
                .withHeader("Accept", equalTo("text/plain"))
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, cacheControlValue)
                        .withHeader("Content-Type", "text/plain")
                        .withHeader("Vary", varyValues)
                        .withBody(textBody)
                )
        );
        stubFor(get(urlPattern)
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, cacheControlValue)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Vary", varyValues)
                        .withBody(jsonBody)
                )
        );
        var textRequest = requestBuilder()
                .header("Accept", "text/plain")
                .build();

        var jsonRequest = requestBuilder()
                .header("Accept", "application/json")
                .build();

        //when + then
        var r1 = send(textRequest);
        var r2 = send(jsonRequest);
        assertThat(r1)
                .isNotCached()
                .hasBody(textBody);
        assertThat(r2)
                .isNotCached()
                .hasBody(jsonBody);

        for (int i = 0; i < count; i++) {
            awaitFor(() -> assertThat(send(textRequest))
                    .isCached()
                    .hasBody(textBody));

            awaitFor(() -> assertThat(send(jsonRequest))
                    .isCached()
                    .hasBody(jsonBody));
        }
    }

    @Test
    default void shouldUpdateExistingCacheWithNoCacheProvided() throws Exception {
        //given
        var urlPattern = urlEqualTo(path());
        stubFor(get(urlPattern)
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=200")
                        .withBody("abc")
                )
        );
        stubFor(get(urlPattern)
                .withHeader(HEADER_CACHE_CONTROL, equalTo("no-cache"))
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=200")
                        .withBody("abc: Updated")
                )
        );

        //when + then
        var r1 = send(requestBuilder().build());
        assertThat(r1).isNotCached().hasBody("abc");

        var r2 = send(requestBuilder().header(HEADER_CACHE_CONTROL, "no-cache").build());
        assertThat(r2).isNotCached().hasBody("abc: Updated");

        awaitFor(() -> {
            var r3 = send(requestBuilder().build());
            assertThat(r3).isCached().hasBody("abc: Updated");
        });
    }

    @Test
    default void shouldRespectMaxAgeRequests() throws Exception {
        //given
        stubFor(get(urlEqualTo(path()))
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=16")
                        .withBody("abc")
                )
        );

        //when + then
        var r1 = send(requestBuilder().build());
        assertThat(r1).isNotCached();

        awaitFor(() -> {
            var r2 = send(requestBuilder().header(HEADER_CACHE_CONTROL, "max-age=8").build());
            assertThat(r2).isCached();
        });

        TimeUnit.SECONDS.sleep(1);

        var r3 = send(requestBuilder().header(HEADER_CACHE_CONTROL, "max-age=1").build());
        assertThat(r3).isNotCached();
    }

    @ParameterizedTest(name = "{0}: Should invalidate existing cache when unsafe HTTP methods are used")
    @ValueSource(strings = {"POST", "PUT", "DELETE"})
    default void shouldInvalidateWhenUnsafe(String method) throws Exception {
        //given
        var urlPattern = urlEqualTo(path());
        String locationPath = path() + "/1";
        String contentLocationPath = path() + "/2";
        URI locationUri = resolve(locationPath);
        URI contentLocationUri = resolve(contentLocationPath);
        stubFor(get(urlPattern)
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=512")
                        .withBody("abc")
                )
        );
        stubFor(get(urlEqualTo(locationPath))
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=512")
                        .withBody("abc1")
                )
        );
        stubFor(get(urlEqualTo(contentLocationPath))
                .willReturn(ok()
                        .withHeader(HEADER_CACHE_CONTROL, "max-age=512")
                        .withBody("abc2")
                )
        );
        stubFor(post(urlPattern).willReturn(created()
                .withHeader("Location", locationPath)
                .withHeader("Content-Location", contentLocationPath)
                .withBody("abc")));

        stubFor(put(urlPattern).willReturn(noContent()
                .withHeader("Location", locationPath)
                .withHeader("Content-Location", contentLocationPath)
        ));
        stubFor(delete(urlPattern).willReturn(noContent()
                .withHeader("Location", locationPath)
                .withHeader("Content-Location", contentLocationPath)
        ));

        var bodyPublisher = BodyPublishers.noBody();
        Duration timeout = Duration.ofMillis(1000);

        //when + then
        List<URI> uris = List.of(resolve(path()), locationUri, contentLocationUri);

        for (URI uri : uris) {
            await().atMost(timeout)
                    .until(() -> send(HttpRequest.newBuilder(uri).build()), isCached());
        }

        send(requestBuilder().method(method, bodyPublisher).build()); // this request should invalidate

        for (URI uri : uris) {
            HttpResponse<String> response = send(HttpRequest.newBuilder(uri).build());
            assertThat(response).isNetwork().isNotCached();
        }
    }

    default String rfc1123Date() {
        return toRFC1123(clock().instant());
    }

    default ResponseDefinitionBuilder ok() {
        return WireMock.ok().withHeader(HEADER_DATE, rfc1123Date());
    }
}
