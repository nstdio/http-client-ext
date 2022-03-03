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
         *
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
         *
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
