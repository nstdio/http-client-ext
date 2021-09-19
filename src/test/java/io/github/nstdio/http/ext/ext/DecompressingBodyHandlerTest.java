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
package io.github.nstdio.http.ext.ext;

import static io.github.nstdio.http.ext.ext.Compression.deflate;
import static io.github.nstdio.http.ext.ext.Compression.gzip;
import static io.github.nstdio.http.ext.ext.DecompressingBodyHandler.decompressionFn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

class DecompressingBodyHandlerTest {

  @ParameterizedTest
  @ValueSource(strings = {"gzip", "x-gzip"})
  void shouldReturnGzipInputStream(String directive) {
    var gzipContent = new ByteArrayInputStream(gzip("abc"));

    //when
    var fn = decompressionFn(directive);
    var inputStream = fn.apply(gzipContent);

    //then
    assertThat(inputStream)
        .isInstanceOf(GZIPInputStream.class);
  }

  @Test
  void shouldReturnDeflateInputStream() {
    var deflateContent = new ByteArrayInputStream(deflate("abc"));

    //when
    var fn = decompressionFn("deflate");
    var inputStream = fn.apply(deflateContent);

    //then
    assertThat(inputStream)
        .isInstanceOf(InflaterInputStream.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"compress", "br"})
  void shouldThrowUnsupportedOperationException(String directive) {
    //when + then
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> decompressionFn(directive))
        .withMessage("Compression directive '%s' is not supported", directive);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "abc", "gz", "a"})
  void shouldThrowIllegalArgumentException(String directive) {
    //when + then
    assertThatIllegalArgumentException()
        .isThrownBy(() -> decompressionFn(directive))
        .withMessage("Unknown compression directive '%s'", directive);
  }
}
