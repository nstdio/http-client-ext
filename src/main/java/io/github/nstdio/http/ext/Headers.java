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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

class Headers {
  static final String HEADER_VARY = "Vary";
  static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
  static final String HEADER_CONTENT_LENGTH = "Content-Length";
  static final String HEADER_CONTENT_TYPE = "Content-Type";
  static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
  static final String HEADER_IF_NONE_MATCH = "If-None-Match";
  static final String HEADER_CACHE_CONTROL = "Cache-Control";
  static final String HEADER_EXPIRES = "Expires";
  static final String HEADER_DATE = "Date";
  static final String HEADER_ETAG = "ETag";
  static final String HEADER_LAST_MODIFIED = "Last-Modified";
  static final String HEADER_WARNING = "Warning";
  static final BiPredicate<String, String> ALLOW_ALL = (s, s2) -> true;
  private static final HttpHeaders EMPTY_HEADERS = HttpHeaders.of(Map.of(), ALLOW_ALL);
  private static final DateTimeFormatter ASCTIME_DATE_TIME = new DateTimeFormatterBuilder()
      .appendPattern("EEE MMM")
      .appendLiteral(' ')
      .padNext(2, ' ')
      .appendValue(ChronoField.DAY_OF_MONTH)
      .appendLiteral(' ')
      .appendPattern("HH:mm:ss yyyy")
      .toFormatter()
      .withLocale(Locale.ENGLISH)
      .withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter IMFFIXDATE_DATE_TIME = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
      .withLocale(Locale.ENGLISH)
      .withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter RFC_830_DATE_TIME = new DateTimeFormatterBuilder()
      .appendPattern("EEEE")
      .appendLiteral(',')
      .appendLiteral(' ')
      .appendPattern("dd-MMM-")
      .appendValueReduced(ChronoField.YEAR_OF_ERA, 2, 2, LocalDate.now().minusYears(50))
      .appendLiteral(' ')
      .appendPattern("HH:mm:ss")
      .appendLiteral(' ')
      .appendLiteral("GMT")
      .toFormatter()
      .withLocale(Locale.ENGLISH)
      .withZone(ZoneOffset.UTC);
  private static final List<DateTimeFormatter> FORMATTERS = List.of(
      IMFFIXDATE_DATE_TIME,
      RFC_830_DATE_TIME,
      ASCTIME_DATE_TIME
  );

  private Headers() {
  }

  static List<String> splitComma(String value) {
    List<String> parts = null;

    int i = 0;
    int len = value.length();
    while (i != -1 && i < len) {
      int j = value.indexOf(',', i);
      boolean found = j != -1;
      String split = (found ? value.substring(i, j) : value.substring(i)).trim();

      if (!split.isEmpty()) {
        if (parts == null) {
          parts = new ArrayList<>(1);
        }

        parts.add(split);
      }

      i = found ? j + 1 : j;
    }

    return parts != null ? parts : List.of();
  }

  static Optional<String> firstValue(HttpHeaders headers, String name) {
    return headers.firstValue(name).filter(not(String::isBlank));
  }

  static Instant parseInstant(String date) {
    for (DateTimeFormatter fmt : FORMATTERS) {
      try {
        return fmt.parse(date, Instant::from);
      } catch (DateTimeParseException ignored) {
      }
    }

    return null;
  }

  static String toRFC1123(Instant instant) {
    return IMFFIXDATE_DATE_TIME.format(instant);
  }

  static boolean isVaryAll(HttpHeaders headers) {
    for (String value : headers.allValues(HEADER_VARY)) {
      if ("*".equals(value)) {
        return true;
      }

      if (splitComma(value).contains("*")) {
        return true;
      }
    }

    return false;
  }

  static HttpHeaders varyHeaders(HttpHeaders request, HttpHeaders response) {
    var varyNames = response.allValues(HEADER_VARY);
    if (varyNames.isEmpty()) {
      return EMPTY_HEADERS;
    }

    HttpHeadersBuilder builder = null;
    for (String varyName : varyNames) {
      for (String s : splitComma(varyName)) {
        var values = request.allValues(s);

        if (!values.isEmpty()) {
          if (builder == null) builder = new HttpHeadersBuilder();
          builder.add(s, values);
        }
      }
    }

    return builder == null ? EMPTY_HEADERS : builder.build();
  }

  static boolean hasConditions(HttpHeaders h) {
    return h.firstValue(HEADER_IF_MODIFIED_SINCE).isPresent()
        || h.firstValue(HEADER_IF_NONE_MATCH).isPresent();
  }

  static Optional<Instant> parseInstant(HttpHeaders headers, String headerName) {
    return headers.firstValue(headerName)
        .filter(not(String::isBlank))
        .map(Headers::parseInstant);
  }

  static List<URI> effectiveUri(HttpHeaders headers, String headerName, URI responseUri) {
    return headers.allValues(headerName)
        .stream()
        .map(s -> effectiveUri(s, responseUri))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  static URI effectiveUri(String s, URI responseUri) {
    if (s == null || s.isBlank())
      return null;
    try {
      URI uri = new URI(s);
      if (uri.getHost() == null) {
        uri = new URI(responseUri.getScheme(), responseUri.getUserInfo(),
            responseUri.getHost(), responseUri.getPort(), uri.getPath(),
            uri.getQuery(), uri.getFragment());
      }
      return uri;
    } catch (URISyntaxException e) {
      return null;
    }
  }
}
