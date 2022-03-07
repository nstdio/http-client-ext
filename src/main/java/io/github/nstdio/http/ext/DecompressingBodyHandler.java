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

import static io.github.nstdio.http.ext.Headers.HEADER_CONTENT_ENCODING;
import static java.util.stream.Collectors.toMap;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.ResponseInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

class DecompressingBodyHandler<T> implements BodyHandler<T> {
    private static final String UNSUPPORTED_DIRECTIVE = "Compression directive '%s' is not supported";
    private static final String UNKNOWN_DIRECTIVE = "Unknown compression directive '%s'";
    private static final UnaryOperator<InputStream> IDENTITY = UnaryOperator.identity();
    private final BodyHandler<T> original;
    private final Options options;
    private final boolean direct;
    private volatile List<String> directives;

    DecompressingBodyHandler(BodyHandler<T> original, Options options) {
        this(Objects.requireNonNull(original), Objects.requireNonNull(options), false);
    }

    private DecompressingBodyHandler(BodyHandler<T> original, Options options, boolean direct) {
        this.original = original;
        this.options = options;
        this.direct = direct;
    }

    static DecompressingBodyHandler<InputStream> ofDirect(Options options) {
        return new DecompressingBodyHandler<>(null, options, true);
    }

    private UnaryOperator<InputStream> chain(UnaryOperator<InputStream> u1, UnaryOperator<InputStream> u2) {
        return in -> u2.apply(u1.apply(in));
    }

    UnaryOperator<InputStream> decompressionFn(String directive) {
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
                if (options.failOnUnsupportedDirectives()) {
                    throw new UnsupportedOperationException(String.format(UNSUPPORTED_DIRECTIVE, directive));
                }
                return IDENTITY;
            default:
                if (options.failOnUnknownDirectives()) {
                    throw new IllegalArgumentException(String.format(UNKNOWN_DIRECTIVE, directive));
                }

                return IDENTITY;
        }
    }

    @Override
    public BodySubscriber<T> apply(ResponseInfo info) {
        var directiveToFn = computeDirectives(info.headers());
        if (directiveToFn.isEmpty()) {
            return original.apply(info);
        }

        var reduced = directiveToFn
                .values()
                .stream()
                .reduce(IDENTITY, this::chain);

        directives = List.copyOf(directiveToFn.keySet());

        if (direct) {
            return directSubscriber(reduced);
        }

        return new DecompressingSubscriber<>(original.apply(info), reduced);
    }

    private BodySubscriber<T> directSubscriber(UnaryOperator<InputStream> reduced) {
        @SuppressWarnings("unchecked")
        var directSubscriber = (BodySubscriber<T>) new InputStreamDecompressingBodySubscriber(reduced);

        return directSubscriber;
    }

    private Map<String, UnaryOperator<InputStream>> computeDirectives(HttpHeaders headers) {
        var encodingOpt = Headers.firstValue(headers, HEADER_CONTENT_ENCODING);
        if (encodingOpt.isEmpty()) {
            return Map.of();
        }

        return Headers.splitComma(encodingOpt.get())
                .map(s -> Map.entry(s, decompressionFn(s)))
                .filter(e -> e.getValue() != IDENTITY)
                .collect(toMap(Entry::getKey, Entry::getValue, (f1, f2) -> f1, LinkedHashMap::new));
    }

    List<String> directives(HttpHeaders headers) {
        if (directives != null)
            return directives;

        return (directives = List.copyOf(computeDirectives(headers).keySet()));
    }

    @Getter
    @Accessors(fluent = true)
    static class Options {
        static final Options LENIENT = new Options(false, false);
        private final boolean failOnUnsupportedDirectives;
        private final boolean failOnUnknownDirectives;

        Options(boolean failOnUnsupportedDirectives, boolean failOnUnknownDirectives) {
            this.failOnUnsupportedDirectives = failOnUnsupportedDirectives;
            this.failOnUnknownDirectives = failOnUnknownDirectives;
        }
    }
}
