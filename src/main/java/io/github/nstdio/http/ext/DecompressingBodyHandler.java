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
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.ResponseInfo;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

final class DecompressingBodyHandler implements BodyHandler<InputStream> {
    private static final String UNSUPPORTED_DIRECTIVE = "Compression directive '%s' is not supported";
    private static final String UNKNOWN_DIRECTIVE = "Unknown compression directive '%s'";

    private final Options options;

    DecompressingBodyHandler(Options config) {
        this.options = config;
    }

    // Visible for testing
    Function<InputStream, InputStream> decompressionFn(String directive) {
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
                if (options.failOnUnsupportedDirectives) {
                    throw new UnsupportedOperationException(String.format(UNSUPPORTED_DIRECTIVE, directive));
                }
                return Function.identity();
            default:
                if (options.failOnUnknownDirectives) {
                    throw new IllegalArgumentException(String.format(UNKNOWN_DIRECTIVE, directive));
                }

                return Function.identity();
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

        var encodings = Headers.splitComma(encodingOpt.get()).collect(toList());

        return encodings
                .stream()
                .map(this::decompressionFn)
                .reduce(Function::andThen)
                .<BodySubscriber<InputStream>>map(DecompressingBodySubscriber::new)
                .orElseGet(BodySubscribers::ofInputStream);
    }

    static class Options {
        private final boolean failOnUnsupportedDirectives;
        private final boolean failOnUnknownDirectives;

        Options(boolean failOnUnsupportedDirectives, boolean failOnUnknownDirectives) {
            this.failOnUnsupportedDirectives = failOnUnsupportedDirectives;
            this.failOnUnknownDirectives = failOnUnknownDirectives;
        }
    }
}
