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

import static io.github.nstdio.http.ext.Headers.splitComma;

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CacheControl {
    public static final CacheControl FORCE_CACHE = builder().onlyIfCached().maxAge(Long.MAX_VALUE).build();
    private static final CacheControl EMPTY = builder().build();

    private static final Pattern VALUE_DIRECTIVES_PATTERN = Pattern.compile("(max-age|max-stale|min-fresh)=(?:([0-9]+)|\"([0-9]+)\")");
    private final boolean noCache;
    private final boolean noStore;
    private final boolean mustRevalidate;

    private final long maxAge;
    private final long maxStale;
    private final long minFresh;

    private final boolean noTransform;
    private final boolean onlyIfCached;

    private CacheControl(boolean noCache, boolean noStore, boolean mustRevalidate, long maxAge, long maxStale,
                         long minFresh, boolean noTransform, boolean onlyIfCached) {
        this.mustRevalidate = mustRevalidate;
        this.noCache = noCache;
        this.noStore = noStore;
        this.maxAge = maxAge;
        this.maxStale = maxStale;
        this.minFresh = minFresh;
        this.noTransform = noTransform;
        this.onlyIfCached = onlyIfCached;
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
        splitComma(value).map(String::toLowerCase).forEach(s -> {
            switch (s) {
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
                case "only-if-cached":
                    builder.onlyIfCached();
                    break;
                default: {
                    parseValue(builder, s);
                    break;
                }
            }
        });
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

    public boolean noCache() {
        return noCache;
    }

    public boolean noStore() {
        return noStore;
    }

    public boolean mustRevalidate() {
        return mustRevalidate;
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

    public boolean noTransform() {
        return noTransform;
    }

    public boolean onlyIfCached() {
        return onlyIfCached;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        if (noCache) sb.append("no-cache");
        if (noStore) appendComma(sb).append("no-store");
        if (mustRevalidate) appendComma(sb).append("must-revalidate");

        if (noTransform) appendComma(sb).append("no-transform");
        if (onlyIfCached) appendComma(sb).append("only-if-cached");

        if (maxAge > -1) appendComma(sb).append("max-age=").append(maxAge);
        if (maxStale > -1) appendComma(sb).append("max-stale=").append(maxStale);
        if (minFresh > -1) appendComma(sb).append("min-fresh=").append(minFresh);

        return sb.toString();
    }

    private StringBuilder appendComma(StringBuilder sb) {
        if (sb.length() > 0) sb.append(", ");
        return sb;
    }

    public static class CacheControlBuilder {
        private boolean noCache;
        private boolean noStore;
        private boolean mustRevalidate;

        private long maxAge = -1;
        private long maxStale = -1;
        private long minFresh = -1;

        private boolean noTransform;
        private boolean onlyIfCached;

        CacheControlBuilder() {
        }

        public CacheControlBuilder noCache() {
            noCache = true;
            return this;
        }

        public CacheControlBuilder noStore() {
            noStore = true;
            return this;
        }

        public CacheControlBuilder mustRevalidate() {
            mustRevalidate = true;
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

        public CacheControlBuilder noTransform() {
            noTransform = true;
            return this;
        }

        public CacheControlBuilder onlyIfCached() {
            onlyIfCached = true;
            return this;
        }

        public CacheControl build() {
            return new CacheControl(noCache, noStore, mustRevalidate, maxAge, maxStale, minFresh, noTransform, onlyIfCached);
        }

        public String toString() {
            return "CacheControl.CacheControlBuilder(noCache=" + this.noCache + ", noStore=" + this.noStore + ", maxAge=" + this.maxAge + ", maxStale=" + this.maxStale + ", minFresh=" + this.minFresh + ", noTransform=" + this.noTransform + ", onlyIfCached=" + this.onlyIfCached + ")";
        }
    }
}
