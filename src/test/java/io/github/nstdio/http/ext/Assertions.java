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

import static io.github.nstdio.http.ext.Properties.duration;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.nstdio.http.ext.Responses.DelegatingHttpResponse;
import org.assertj.core.api.ObjectAssert;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ThrowingRunnable;

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class Assertions {
    static final Duration POLL_INTERVAL = duration("client.test.pool.interval")
            .orElseGet(() -> Duration.ofMillis(5));
    /**
     * The maximum time to wait until cache gets written.
     */
    static final Duration CACHE_WRITE_DELAY = duration("client.test.cache.write.delay")
            .orElseGet(() -> Duration.ofMillis(500));

    public static <T> HttpResponseAssertion<T> assertThat(HttpResponse<T> r) {
        return new HttpResponseAssertion<>(r);
    }

    public static HttpHeadersAssertion assertThat(HttpHeaders h) {
        return new HttpHeadersAssertion(h);
    }

    public static <K, V> LruMultimapAssertion<K, V> assertThat(LruMultimap<K, V> m) {
        return new LruMultimapAssertion<>(m);
    }

    static void awaitFor(ThrowingRunnable r) {
        await().untilAsserted(r);
    }

    static ConditionFactory await() {
        return Awaitility.await().pollInterval(POLL_INTERVAL).atMost(CACHE_WRITE_DELAY);
    }

    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static class LruMultimapAssertion<K, V> extends ObjectAssert<LruMultimap<K, V>> {
        public LruMultimapAssertion(LruMultimap<K, V> m) {
            super(m);
        }

        public LruMultimapAssertion<K, V> hasSize(int size) {
            org.assertj.core.api.Assertions.assertThat(actual.size()).isEqualTo(size);
            return this;
        }

        public LruMultimapAssertion<K, V> hasMapSize(int size) {
            org.assertj.core.api.Assertions.assertThat(actual.mapSize()).isEqualTo(size);
            return this;
        }

        public LruMultimapAssertion<K, V> hasOnlyValue(K k, V v, int index) {
            org.assertj.core.api.Assertions.assertThat(actual.getSingle(k, vs -> index))
                    .isEqualTo(v);
            return this;
        }

        public LruMultimapAssertion<K, V> isEmpty() {
            return hasSize(0).hasMapSize(0);
        }
    }

    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static class HttpHeadersAssertion extends ObjectAssert<HttpHeaders> {
        HttpHeadersAssertion(HttpHeaders headers) {
            super(headers);
        }

        public HttpHeadersAssertion hasHeaderWithValues(String header, String... values) {
            org.assertj.core.api.Assertions.assertThat(actual.allValues(header))
                    .containsExactlyInAnyOrder(values);
            return this;
        }

        public HttpHeadersAssertion hasHeaderWithOnlyValue(String header, String value) {
            org.assertj.core.api.Assertions.assertThat(actual.allValues(header))
                    .containsExactly(value);
            return this;
        }

        public HttpHeadersAssertion hasNoHeader(String header) {
            org.assertj.core.api.Assertions.assertThat(actual.allValues(header))
                    .isEmpty();
            return this;
        }

        public HttpHeadersAssertion isEqualTo(HttpHeaders other) {
            org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(other);
            return this;
        }
    }

    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static class HttpRequestAssertion extends ObjectAssert<HttpRequest> {

        public HttpRequestAssertion(HttpRequest httpRequest) {
            super(httpRequest);
        }

        public HttpRequestAssertion isEqualTo(HttpRequest other) {
            org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(other);
            return this;
        }
    }

    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static class HttpResponseAssertion<T> extends ObjectAssert<HttpResponse<T>> {
        HttpResponseAssertion(HttpResponse<T> tHttpResponse) {
            super(tHttpResponse);
        }

        public HttpResponseAssertion<T> hasStatusCode(int statusCode) {
            org.assertj.core.api.Assertions.assertThat(actual.statusCode())
                    .withFailMessage(() -> String.format("Expecting %s to have status code %d, but has %d", actual, statusCode, actual.statusCode()))
                    .isEqualTo(statusCode);

            return this;
        }

        public HttpResponseAssertion<T> isNetwork() {
            org.assertj.core.api.Assertions.assertThat(actual)
                    .satisfiesAnyOf(
                            this::assertJdk,
                            response -> {
                                assertInstanceOf(DelegatingHttpResponse.class, response);
                                assertJdk(((DelegatingHttpResponse<T>) response).delegate());
                            }
                    );

            return this;
        }

        private void assertJdk(HttpResponse<T> response) {
            assertEquals("jdk.internal.net.http", response.getClass().getPackageName());
        }

        public HttpResponseAssertion<T> isNotNetwork() {
            org.assertj.core.api.Assertions.assertThat(actual.getClass().getPackageName())
                    .withFailMessage("The response is network!")
                    .isNotEqualTo("jdk.internal.net.http");

            return this;
        }

        public HttpResponseAssertion<T> isNotCached() {
            org.assertj.core.api.Assertions.assertThat(actual.getClass().getCanonicalName())
                    .withFailMessage("The response is cached!")
                    .isNotEqualTo("io.github.nstdio.http.ext.CachedHttpResponse");

            return this;
        }

        public HttpResponseAssertion<T> isCached() {
            org.assertj.core.api.Assertions.assertThat(actual)
                    .satisfiesAnyOf(
                            response -> assertInstanceOf(CachedHttpResponse.class, response),
                            response -> {
                                assertInstanceOf(DelegatingHttpResponse.class, response);
                                HttpResponse<T> delegate = ((DelegatingHttpResponse<T>) response).delegate();
                                assertInstanceOf(CachedHttpResponse.class, delegate);
                            }
                    );
            return this;
        }

        public HttpResponseAssertion<T> hasBody(T body) {
            org.assertj.core.api.Assertions.assertThat(actual.body()).isEqualTo(body);
            return this;
        }

        public HttpResponseAssertion<T> isSemanticallyEqualTo(HttpResponse<T> other) {
            assertSoftly(softly -> {
                softly.assertThat(actual.statusCode())
                        .describedAs("status code doesn't equal")
                        .isEqualTo(other.statusCode());
                softly.assertThat(actual.headers())
                        .describedAs("headers doesn't equal")
                        .isEqualTo(other.headers());

                softly.assertThat(actual.body())
                        .describedAs("body doesn't equal")
                        .isEqualTo(other.body());

                softly.assertThat(actual.uri())
                        .describedAs("uri doesn't equal")
                        .isEqualTo(other.uri());
                softly.assertThat(actual.version())
                        .describedAs("version doesn't equal")
                        .isEqualTo(other.version());

                // TODO: store ssl session
                // softly.assertThat(actual.sslSession()).isEqualTo(other.sslSession());
            });
            return this;
        }

        public HttpResponseAssertion<T> hasHeader(String header, String value) {
            assertThat(actual.headers())
                    .hasHeaderWithValues(header, value);
            return this;
        }

        public HttpResponseAssertion<T> hasNoHeader(String headers) {
            assertThat(actual.headers())
                    .hasNoHeader(headers);
            return this;
        }
    }
}
