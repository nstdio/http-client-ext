package io.github.nstdio.http.ext;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.assertj.core.api.ObjectAssert;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;

public final class Assertions {
    public static <T> HttpResponseAssertion<T> assertThat(HttpResponse<T> r) {
        return new HttpResponseAssertion<>(r);
    }

    public static HttpHeadersAssertion assertThat(HttpHeaders h) {
        return new HttpHeadersAssertion(h);
    }

    public static <K, V> LruMultimapAssertion<K, V> assertThat(LruMultimap<K, V> m) {
        return new LruMultimapAssertion<>(m);
    }

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
    }

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
            org.assertj.core.api.Assertions.assertThat(actual.getClass().getPackageName())
                    .withFailMessage("The response is cached!")
                    .isEqualTo("jdk.internal.net.http");

            return this;
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
            org.assertj.core.api.Assertions.assertThat(actual).matches(r -> Matchers.isCached().matches(r), "is cached");
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
    }
}
