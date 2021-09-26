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

import io.github.nstdio.http.ext.DecompressingBodyHandler.Options;

import java.io.InputStream;
import java.net.http.HttpResponse.BodyHandler;

/**
 * Implementations of {@code BodyHandler}'s.
 */
public final class BodyHandlers {
    private BodyHandlers() {
    }

    /**
     * Wraps response body {@code InputStream} in on-the-fly decompressing {@code InputStream} in accordance with
     * <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-14.11">Content-Encoding</a> header semantics.
     *
     * @return The decompressing body handler.
     */
    public static BodyHandler<InputStream> ofDecompressing() {
        return new DecompressingBodyHandlerBuilder().build();
    }

    /**
     * Creates new {@code DecompressingBodyHandlerBuilder} instance.
     *
     * @return The builder for decompressing body handler.
     */
    public static DecompressingBodyHandlerBuilder decompressingBuilder() {
        return new DecompressingBodyHandlerBuilder();
    }

    /**
     * The builder for decompressing body handler.
     */
    public static final class DecompressingBodyHandlerBuilder {
        private boolean failOnUnsupportedDirectives = true;
        private boolean failOnUnknownDirectives = true;

        /**
         * Sets whether throw exception when compression directive not supported or not.
         *
         * @param failOnUnsupportedDirectives Whether throw exception when compression directive not supported or not
         * @return this for fluent chaining.
         */
        public DecompressingBodyHandlerBuilder failOnUnsupportedDirectives(boolean failOnUnsupportedDirectives) {
            this.failOnUnsupportedDirectives = failOnUnsupportedDirectives;
            return this;
        }

        /**
         * Sets whether throw exception when unknown compression directive encountered or not.
         *
         * @param failOnUnknownDirectives Whether throw exception when unknown compression directive encountered or not
         * @return this for fluent chaining.
         */
        public DecompressingBodyHandlerBuilder failOnUnknownDirectives(boolean failOnUnknownDirectives) {
            this.failOnUnknownDirectives = failOnUnknownDirectives;
            return this;
        }

        /**
         * Creates the new decompressing body handler.
         *
         * @return The builder for decompressing body handler.
         */
        public BodyHandler<InputStream> build() {
            var config = new Options(failOnUnsupportedDirectives, failOnUnknownDirectives);

            return new DecompressingBodyHandler(config);
        }
    }
}
