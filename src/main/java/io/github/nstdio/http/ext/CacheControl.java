/*
 * Copyright (C) 2022-2025 the original author or authors.
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.nstdio.http.ext.Headers.splitComma;

@SuppressWarnings("WeakerAccess")
public class CacheControl {
  private static final CacheControl EMPTY = builder().build();
  private static final Pattern VALUE_DIRECTIVES_PATTERN = Pattern.compile("(max-age|max-stale|min-fresh|stale-if-error|stale-while-revalidate)=(?:([0-9]+)|\"([0-9]+)\")");

  //@formatter:off
  private static final int NO_CACHE         = 1 << 1;
  private static final int NO_STORE         = 1 << 2;
  private static final int MUST_REVALIDATE  = 1 << 3;
  private static final int NO_TRANSFORM     = 1 << 4;
  private static final int IMMUTABLE        = 1 << 5;
  private static final int ONLY_IF_CACHED   = 1 << 6;
  private static final int MUST_UNDERSTAND  = 1 << 7;
  //@formatter:on

  public static final CacheControl FORCE_CACHE = builder().onlyIfCached().maxAge(Long.MAX_VALUE).build();

  private final int flags;

  private final long maxAge;
  private final long maxStale;
  private final long minFresh;

  private final long staleIfError;
  private final long staleWhileRevalidate;

  private CacheControl(int flags, long maxAge, long maxStale,
                       long minFresh, long staleIfError, long staleWhileRevalidate) {
    this.flags = flags;
    this.maxAge = maxAge;
    this.maxStale = maxStale;
    this.minFresh = minFresh;
    this.staleIfError = staleIfError;
    this.staleWhileRevalidate = staleWhileRevalidate;
  }

  public static CacheControl of(HttpRequest request) {
    return of(request.headers());
  }

  public static CacheControl of(HttpHeaders headers) {
    var values = headers.allValues(Headers.HEADER_CACHE_CONTROL);

    return values.size() == 1 ? parse(values.get(0)) : EMPTY;
  }

  public static CacheControl parse(String value) {
    var builder = builder();
    for (String s : splitComma(value)) {
      switch (s.toLowerCase()) {
        case "no-cache":
          builder.noCache();
          break;
        case "no-store":
          builder.noStore();
          break;
        case "must-revalidate":
          builder.mustRevalidate();
          break;
        case "no-transform":
          builder.noTransform();
          break;
        case "immutable":
          builder.immutable();
          break;
        case "only-if-cached":
          builder.onlyIfCached();
          break;
        case "must-understand":
          builder.mustUnderstand();
          break;
        default: {
          parseValue(builder, s);
          break;
        }
      }
    }
    return builder.build();
  }

  private static void parseValue(CacheControlBuilder builder, String s) {
    Matcher m = VALUE_DIRECTIVES_PATTERN.matcher(s);
    if (m.matches()) {
      String directive = m.group(1);
      long sec;
      try {
        int g = m.start(2) != -1 ? 2 : 3;
        sec = Long.parseLong(s, m.start(g), m.end(g), 10);
      } catch (NumberFormatException e) {
        // overflow
        return;
      }

      switch (directive) {
        case "max-age":
          builder.maxAge(sec);
          break;
        case "max-stale":
          builder.maxStale(sec);
          break;
        case "min-fresh":
          builder.minFresh(sec);
          break;
        case "stale-if-error":
          builder.staleIfError(sec);
          break;
        case "stale-while-revalidate":
          builder.staleWhileRevalidate(sec);
          break;
        default:
          // Since pattern already matched VALUE_DIRECTIVES_PATTERN
          // there is misalignment between pattern and cases
          throw new AssertionError("unknown directive");
      }
    }
  }

  public static CacheControlBuilder builder() {
    return new CacheControlBuilder();
  }

  private static long convert(long value, TimeUnit unit) {
    if (value == -1) {
      return -1;
    }
    return unit.convert(value, TimeUnit.SECONDS);
  }

  private static boolean isSet(int flag, int mask) {
    return (flag & mask) > 0;
  }

  private static int set(int flag, int mask) {
    return flag | mask;
  }

  public boolean noCache() {
    return isSet(flags, NO_CACHE);
  }

  public boolean noStore() {
    return !mustUnderstand() && isSet(flags, NO_STORE);
  }

  public boolean mustRevalidate() {
    return isSet(flags, MUST_REVALIDATE);
  }

  public long maxAge() {
    return maxAge;
  }

  public long maxAge(TimeUnit unit) {
    return convert(maxAge, unit);
  }

  public long maxStale() {
    return maxStale;
  }

  public long maxStale(TimeUnit unit) {
    return convert(maxStale, unit);
  }

  public long minFresh() {
    return minFresh;
  }

  public long minFresh(TimeUnit unit) {
    return convert(minFresh, unit);
  }

  public long staleIfError() {
    return staleIfError;
  }

  public long staleIfError(TimeUnit unit) {
    return convert(staleIfError, unit);
  }

  public long staleWhileRevalidate() {
    return staleWhileRevalidate;
  }

  public long staleWhileRevalidate(TimeUnit unit) {
    return convert(staleWhileRevalidate, unit);
  }

  public boolean noTransform() {
    return isSet(flags, NO_TRANSFORM);
  }

  public boolean immutable() {
    return isSet(flags, IMMUTABLE);
  }

  public boolean onlyIfCached() {
    return isSet(flags, ONLY_IF_CACHED);
  }

  public boolean mustUnderstand() {
    return isSet(flags, MUST_UNDERSTAND);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    if (noCache()) sb.append("no-cache");
    if (isSet(flags, NO_STORE)) appendComma(sb).append("no-store");
    if (mustRevalidate()) appendComma(sb).append("must-revalidate");

    if (noTransform()) appendComma(sb).append("no-transform");
    if (immutable()) appendComma(sb).append("immutable");
    if (onlyIfCached()) appendComma(sb).append("only-if-cached");
    if (mustUnderstand()) appendComma(sb).append("must-understand");

    if (maxAge > -1) appendComma(sb).append("max-age=").append(maxAge);
    if (maxStale > -1) appendComma(sb).append("max-stale=").append(maxStale);
    if (minFresh > -1) appendComma(sb).append("min-fresh=").append(minFresh);
    if (staleIfError > -1) appendComma(sb).append("stale-if-error=").append(staleIfError);
    if (staleWhileRevalidate > -1) appendComma(sb).append("stale-while-revalidate=").append(staleWhileRevalidate);

    return sb.toString();
  }

  private StringBuilder appendComma(StringBuilder sb) {
    if (sb.length() > 0) sb.append(", ");
    return sb;
  }

  public static class CacheControlBuilder {
    private int flags;

    private long maxAge = -1;
    private long maxStale = -1;
    private long minFresh = -1;
    private long staleIfError = -1;
    private long staleWhileRevalidate = -1;

    CacheControlBuilder() {
    }

    public CacheControlBuilder noCache() {
      flags = set(flags, NO_CACHE);
      return this;
    }

    public CacheControlBuilder noStore() {
      flags = set(flags, NO_STORE);
      return this;
    }

    public CacheControlBuilder mustRevalidate() {
      flags = set(flags, MUST_REVALIDATE);
      return this;
    }

    public CacheControlBuilder maxAge(long maxAge) {
      this.maxAge = maxAge;
      return this;
    }

    public CacheControlBuilder maxStale(long maxStale) {
      this.maxStale = maxStale;
      return this;
    }

    public CacheControlBuilder minFresh(long minFresh) {
      this.minFresh = minFresh;
      return this;
    }

    public CacheControlBuilder staleIfError(long deltaSeconds) {
      this.staleIfError = deltaSeconds;
      return this;
    }

    public CacheControlBuilder staleWhileRevalidate(long deltaSeconds) {
      staleWhileRevalidate = deltaSeconds;
      return this;
    }

    public CacheControlBuilder noTransform() {
      flags = set(flags, NO_TRANSFORM);
      return this;
    }

    public CacheControlBuilder immutable() {
      flags = set(flags, IMMUTABLE);
      return this;
    }

    public CacheControlBuilder onlyIfCached() {
      flags = set(flags, ONLY_IF_CACHED);
      return this;
    }

    public CacheControlBuilder mustUnderstand() {
      flags = set(flags, MUST_UNDERSTAND);
      return this;
    }

    public CacheControl build() {
      return new CacheControl(flags, maxAge, maxStale, minFresh, staleIfError, staleWhileRevalidate);
    }
  }
}
