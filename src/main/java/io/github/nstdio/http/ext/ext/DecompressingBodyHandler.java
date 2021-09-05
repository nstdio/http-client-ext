package io.github.nstdio.http.ext.ext;

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
