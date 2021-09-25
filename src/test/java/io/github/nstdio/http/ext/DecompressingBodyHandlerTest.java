/*
 * Copyright (C) 2021 Edgar Asatryan
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.nstdio.http.ext;

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

import static io.github.nstdio.http.ext.Compression.deflate;
import static io.github.nstdio.http.ext.Compression.gzip;
import static org.assertj.core.api.Assertions.*;

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
