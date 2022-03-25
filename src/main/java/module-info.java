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

import io.github.nstdio.http.ext.spi.CompressionFactory;
import io.github.nstdio.http.ext.spi.IdentityCompressionFactory;
import io.github.nstdio.http.ext.spi.JdkCompressionFactory;
import io.github.nstdio.http.ext.spi.OptionalBrotliCompressionFactory;

module http.client.ext {
    uses CompressionFactory;

    requires transitive java.net.http;

    requires transitive com.fasterxml.jackson.core;
    requires transitive com.fasterxml.jackson.databind;

    requires static lombok;
    requires static com.aayushatharva.brotli4j;
    requires static org.brotli.dec;

    exports io.github.nstdio.http.ext;
    exports io.github.nstdio.http.ext.spi;

    provides CompressionFactory with JdkCompressionFactory,
            IdentityCompressionFactory,
            OptionalBrotliCompressionFactory;
}
