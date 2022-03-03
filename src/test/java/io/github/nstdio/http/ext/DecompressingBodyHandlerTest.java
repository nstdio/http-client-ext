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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.nstdio.http.ext.DecompressingBodyHandler.Options;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

class DecompressingBodyHandlerTest {

    private DecompressingBodyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DecompressingBodyHandler(new Options(true, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gzip", "x-gzip"})
    void shouldReturnGzipInputStream(String directive) {
        var gzipContent = new ByteArrayInputStream(gzip("abc"));

        //when
        var fn = handler.decompressionFn(directive);
        var inputStream = fn.apply(gzipContent);

        //then
        assertThat(inputStream)
                .isInstanceOf(GZIPInputStream.class);
    }

    @Test
    void shouldReturnDeflateInputStream() {
        var deflateContent = new ByteArrayInputStream(deflate("abc"));

        //when
        var fn = handler.decompressionFn("deflate");
        var inputStream = fn.apply(deflateContent);

        //then
        assertThat(inputStream)
                .isInstanceOf(InflaterInputStream.class);
    }


    @Nested
    class FailureControlOptions {
        @ParameterizedTest
        @ValueSource(strings = {"compress", "br"})
        void shouldThrowUnsupportedOperationException(String directive) {
            //given
            var options = new Options(true, true);
            var handler = new DecompressingBodyHandler(options);

            //when + then
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> handler.decompressionFn(directive))
                    .withMessage("Compression directive '%s' is not supported", directive);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "abc", "gz", "a"})
        void shouldThrowIllegalArgumentException(String directive) {
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
            var handler = new DecompressingBodyHandler(options);
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
            var handler = new DecompressingBodyHandler(options);
            var in = InputStream.nullInputStream();

            //when
            var fn = handler.decompressionFn(directive);
            var actual = fn.apply(in);

            //then
            assertThat(actual).isSameAs(in);
        }
    }
}
