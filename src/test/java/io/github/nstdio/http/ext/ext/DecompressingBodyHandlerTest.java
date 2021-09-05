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
