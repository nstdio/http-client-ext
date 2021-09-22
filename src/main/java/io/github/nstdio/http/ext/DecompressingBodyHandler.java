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

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

final class DecompressingBodyHandler implements BodyHandler<InputStream> {

  private static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
  private static final Pattern COMMA_PATTERN = Pattern.compile(",", Pattern.LITERAL);
  private static final String UNSUPPORTED_DIRECTIVE = "Compression directive '%s' is not supported";
  private static final String UNKNOWN_DIRECTIVE = "Unknown compression directive '%s'";

  // Visible for testing
  static Function<InputStream, InputStream> decompressionFn(String directive) {
    switch (directive) {
      case "x-gzip":
      case "gzip":
        return in -> {
          try {
            return new GZIPInputStream(in);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        };
      case "deflate":
        return InflaterInputStream::new;
      case "compress":
      case "br":
        throw new UnsupportedOperationException(String.format(UNSUPPORTED_DIRECTIVE, directive));
      default:
        throw new IllegalArgumentException(String.format(UNKNOWN_DIRECTIVE, directive));
    }
  }

  @Override
  public BodySubscriber<InputStream> apply(ResponseInfo responseInfo) {
    var encodingOpt = responseInfo
        .headers()
        .firstValue(HEADER_CONTENT_ENCODING);

    if (encodingOpt.isEmpty()) {
      return BodySubscribers.ofInputStream();
    }

    var encodings = COMMA_PATTERN
        .splitAsStream(encodingOpt.get())
        .map(String::trim)
        .filter(not(String::isEmpty))
        .collect(toList());

    return encodings
        .stream()
        .map(DecompressingBodyHandler::decompressionFn)
        .reduce(Function::andThen)
        .<BodySubscriber<InputStream>>map(DecompressingBodySubscriber::new)
        .orElseGet(BodySubscribers::ofInputStream);
  }

}
