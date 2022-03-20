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

import static io.github.nstdio.http.ext.Compression.deflate;
import static io.github.nstdio.http.ext.Compression.gzip;
import static io.github.nstdio.http.ext.Headers.ALLOW_ALL;
import static io.github.nstdio.http.ext.Headers.HEADER_CONTENT_ENCODING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.github.nstdio.http.ext.DecompressingBodyHandler.Options;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

class DecompressingBodyHandlerTest {

    private DecompressingBodyHandler<?> handler;
    private BodyHandler<Object> mockHandler;
    private BodySubscriber<Object> mockSubscriber;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockHandler = Mockito.mock(BodyHandler.class);
        mockSubscriber = Mockito.mock(BodySubscriber.class);
        handler = new DecompressingBodyHandler<>(mockHandler, new Options(false, false));
    }

    @AfterEach
    void tearDown() {
        verifyNoInteractions(mockSubscriber);
    }

    @Test
    void shouldReturnOriginalSub() {
        //given
        ImmutableResponseInfo responseInfo = ImmutableResponseInfo.builder()
                .headers(HttpHeaders.of(Map.of(), ALLOW_ALL))
                .build();
        given(mockHandler.apply(responseInfo)).willReturn(mockSubscriber);

        //when
        BodySubscriber<?> actual = handler.apply(responseInfo);

        //then
        assertThat(actual).isSameAs(mockSubscriber);
        verify(mockHandler).apply(responseInfo);
        verifyNoMoreInteractions(mockHandler);
    }

    @Test
    void shouldReturnDirectSubscriptionWhenDirect() {
        Options options = new Options(false, false);
        handler = DecompressingBodyHandler.ofDirect(options);

        ImmutableResponseInfo responseInfo = ImmutableResponseInfo.builder()
                .headers(new HttpHeadersBuilder().add("Content-Encoding", "gzip").build())
                .build();

        //when
        BodySubscriber<?> actual = handler.apply(responseInfo);

        //then
        assertThat(actual).isExactlyInstanceOf(AsyncMappingSubscriber.class);
    }

    @Test
    void shouldReturnOriginalSubWhenDirectivesUnsupported() {
        //given
        ImmutableResponseInfo responseInfo = ImmutableResponseInfo.builder()
                .headers(new HttpHeadersBuilder().add(HEADER_CONTENT_ENCODING, "compress,br,identity1,abc").build())
                .build();
        given(mockHandler.apply(responseInfo)).willReturn(mockSubscriber);

        //when
        BodySubscriber<?> actual = handler.apply(responseInfo);

        //then
        assertThat(actual).isSameAs(mockSubscriber);
        verify(mockHandler).apply(responseInfo);
        verifyNoMoreInteractions(mockHandler);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gzip", "x-gzip"})
    void shouldReturnGzipInputStream(String directive) throws IOException {
        var gzipContent = new ByteArrayInputStream(gzip("abc"));

        //when
        var fn = handler.decompressionFn(directive);
        var in = fn.apply(gzipContent);

        //then
        assertThat(in).isInstanceOf(GZIPInputStream.class);
        assertThat(IOUtils.toString(in, StandardCharsets.UTF_8)).isEqualTo("abc");
    }

    @Test
    void shouldReturnDeflateInputStream() throws IOException {
        var deflateContent = new ByteArrayInputStream(deflate("abc"));

        //when
        var fn = handler.decompressionFn("deflate");
        var in = fn.apply(deflateContent);

        //then
        assertThat(in).isInstanceOf(InflaterInputStream.class);
        assertThat(IOUtils.toString(in, StandardCharsets.UTF_8)).isEqualTo("abc");
    }

    @Nested
    class FailureControlOptionsTest {

        @ParameterizedTest
        @ValueSource(strings = {"compress", "br"})
        void shouldThrowUnsupportedOperationException(String directive) {
            //given
            var options = new Options(true, true);
            var handler = new DecompressingBodyHandler<>(mockHandler, options);

            //when + then
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> handler.decompressionFn(directive))
                    .withMessage("Compression directive '%s' is not supported", directive);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "abc", "gz", "a"})
        void shouldThrowIllegalArgumentException(String directive) {
            var handler = new DecompressingBodyHandler<>(mockHandler, new Options(true, true));
            //when + then
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> handler.decompressionFn(directive))
                    .withMessage("Unknown compression directive '%s'", directive);
        }

        @ParameterizedTest
        @ValueSource(strings = {"compress", "br"})
        @DisplayName("Should not throw exception when 'failOnUnsupportedDirectives' is 'false'")
        void shouldNotThrowUnsupportedOperationException(String directive) {
            //given
            var options = new Options(false, true);
            var handler = new DecompressingBodyHandler<>(mockHandler, options);
            var in = InputStream.nullInputStream();

            //when
            var fn = handler.decompressionFn(directive);
            var actual = fn.apply(in);

            //then
            assertThat(actual).isSameAs(in);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "abc", "gz", "a"})
        @DisplayName("Should not throw exception when 'failOnUnknownDirectives' is 'false'")
        void shouldNotIllegalArgumentException(String directive) {
            //given
            var options = new Options(true, false);
            var handler = new DecompressingBodyHandler<>(mockHandler, options);
            var in = InputStream.nullInputStream();

            //when
            var fn = handler.decompressionFn(directive);
            var actual = fn.apply(in);

            //then
            assertThat(actual).isSameAs(in);
        }
    }
}
