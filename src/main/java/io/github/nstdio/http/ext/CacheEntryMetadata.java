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

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.ResponseInfo;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.github.nstdio.http.ext.Headers.HEADER_DATE;
import static io.github.nstdio.http.ext.Headers.HEADER_ETAG;
import static io.github.nstdio.http.ext.Headers.HEADER_EXPIRES;
import static io.github.nstdio.http.ext.Headers.HEADER_LAST_MODIFIED;
import static io.github.nstdio.http.ext.Headers.HEADER_WARNING;
import static io.github.nstdio.http.ext.Headers.parseInstant;
import static io.github.nstdio.http.ext.Headers.toRFC1123;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class CacheEntryMetadata {
  private final HttpRequest request;
  private final Clock clock;

  private HttpHeaders varyHeaders;
  private CacheControl responseCacheControl;

  private Optional<Instant> date;
  private Optional<String> dateString;
  private Optional<Instant> expires;
  private Optional<Instant> lastModified;

  private long maxAge;
  private long ageHeaderValue;

  private ResponseInfo response;
  private long requestTimeMs;
  private long responseTimeMs;

  CacheEntryMetadata(long requestTimeMs, long responseTimeMs, ResponseInfo response, HttpRequest request, Clock clock) {
    this.response = response;
    this.request = request;
    this.clock = clock;

    this.requestTimeMs = requestTimeMs;
    this.responseTimeMs = responseTimeMs;

    initialize();
  }

  static CacheEntryMetadata of(long requestTimeMs, long responseTimeMs, ResponseInfo info, HttpRequest request, Clock clock) {
    var m = new CacheEntryMetadata(requestTimeMs, responseTimeMs, info, request, clock);
    m.updateWarnings();
    return m;
  }

  private void initialize() {
    var requestHeaders = request.headers();
    var responseHeaders = response.headers();

    varyHeaders = Headers.varyHeaders(requestHeaders, responseHeaders);

    responseCacheControl = CacheControl.of(responseHeaders);

    dateString = responseHeaders.firstValue(HEADER_DATE);
    date = dateString.map(Headers::parseInstant);
    expires = parseInstant(responseHeaders, HEADER_EXPIRES);
    lastModified = parseInstant(responseHeaders, HEADER_LAST_MODIFIED);

    maxAge = calculateFreshnessLifetime() * 1000;
    ageHeaderValue = responseHeaders.firstValueAsLong("Age").orElse(0) * 1000;
  }

  private long calculateFreshnessLifetime() {
    if (responseCacheControl.maxAge() != -1) {
      return responseCacheControl.maxAge();
    }

    if (expires.isPresent()) {
      return effectiveDate().until(expires.get(), ChronoUnit.SECONDS);
    }

    if (lastModified.isPresent() && request.uri().getQuery() == null) {
      var l = lastModified.get().until(effectiveDate(), ChronoUnit.SECONDS) / 10;
      return l > 0 ? l : -1;
    }

    return -1;
  }

  private Instant effectiveDate() {
    return date.orElseGet(() -> Instant.ofEpochMilli(responseTimeMs));
  }

  void updateWarnings() {
    if (isFreshnessHeuristicsUsed()) {
      long dayMillis = 60L * 60 * 24 * 1000;
      if (maxAge > dayMillis && age(MILLISECONDS) > dayMillis) {
        synchronized (this) {
          boolean add = true;
          HttpHeaders responseHeaders = response.headers();
          for (String warn : responseHeaders.allValues(HEADER_WARNING)) {
            if (warn.startsWith("113 ")) {
              add = false;
              break;
            }
          }

          if (add) {
            HttpHeaders newHeaders = new HttpHeadersBuilder(responseHeaders)
                .add(HEADER_WARNING, "113 - \"Heuristic Expiration\"")
                .build();

            response = ImmutableResponseInfo.toBuilder(response)
                .headers(newHeaders)
                .build();
          }
        }
      }
    }
  }

  private boolean isFreshnessHeuristicsUsed() {
    return maxAge > 0
        && responseCacheControl.maxAge() == -1
        && expires.isEmpty()
        && lastModified.isPresent();
  }

  ResponseInfo response() {
    return response;
  }

  CacheControl responseCacheControl() {
    return responseCacheControl;
  }

  HttpRequest request() {
    return request;
  }

  HttpHeaders varyHeaders() {
    return varyHeaders;
  }

  boolean isFresh(CacheControl requestCacheControl) {
    long max;
    if ((max = maxAge) <= 0) {
      return false;
    }

    long age = age(MILLISECONDS);

    long rMaxAge = requestCacheControl.maxAge(MILLISECONDS);
    if (rMaxAge != -1 && rMaxAge < age) {
      return false;
    }

    long maxStale = requestCacheControl.maxStale(MILLISECONDS);
    if (maxStale != -1 && age > max) {
      return !responseCacheControl.mustRevalidate() && maxStale > age - max;
    }

    long minFresh = requestCacheControl.minFresh(MILLISECONDS);
    if (minFresh != -1 && minFresh < age) {
      return false;
    }

    return age < max;
  }

  @SuppressWarnings("SameParameterValue")
  long age(TimeUnit unit) {
    long now = clock.millis();
    long date = this.date
        .map(Instant::toEpochMilli)
        .orElse(now);

    long apparentAge = Math.max(0, responseTimeMs - date);
    long responseDelay = responseTimeMs - requestTimeMs;
    long correctedAgeValue = ageHeaderValue + responseDelay;
    long correctedInitialAge = Math.max(apparentAge, correctedAgeValue);
    long residentTime = now - responseTimeMs;
    long currentAge = correctedInitialAge + residentTime;

    return unit.convert(currentAge, MILLISECONDS);
  }

  long maxAge() {
    return maxAge;
  }

  long staleFor() {
    return age(MILLISECONDS) - maxAge;
  }

  long requestTime() {
    return requestTimeMs;
  }

  long responseTime() {
    return responseTimeMs;
  }

  Optional<String> etag() {
    return response.headers().firstValue(HEADER_ETAG);
  }

  Optional<String> date() {
    if (maxAge > 0) {
      return dateString.or(() -> Optional.of(toRFC1123(Instant.ofEpochMilli(responseTimeMs))));
    }

    return Optional.empty();
  }

  synchronized void update(HttpHeaders responseHeaders, long requestTimeMs, long responseTimeMs) {
    this.requestTimeMs = requestTimeMs;
    this.responseTimeMs = responseTimeMs;

    var headersBuilder = new HttpHeadersBuilder(response.headers());

    for (String warn : response.headers().allValues(HEADER_WARNING)) {
      if (warn.startsWith("1")) {
        headersBuilder.remove(HEADER_WARNING, warn);
      }
    }

    responseHeaders.map().forEach(headersBuilder::set);

    response = ImmutableResponseInfo.toBuilder(response).headers(headersBuilder.build()).build();

    initialize();
  }

  boolean isApplicable() {
    return maxAge > 0 || etag().isPresent();
  }
}
