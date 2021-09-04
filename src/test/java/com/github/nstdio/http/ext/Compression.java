package com.github.nstdio.http.ext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.function.UnaryOperator;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

public class Compression {

  public static byte[] gzip(String in) {
    return compress(in, out -> {
      try {
        return new GZIPOutputStream(out);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }

  public static byte[] deflate(String in) {
    return compress(in, DeflaterOutputStream::new);
  }

  private static byte[] compress(String in, UnaryOperator<OutputStream> compressorCreator) {
    try (var out = new ByteArrayOutputStream(); var compressor = compressorCreator.apply(out)) {
      compressor.write(in.getBytes());
      compressor.flush();
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
