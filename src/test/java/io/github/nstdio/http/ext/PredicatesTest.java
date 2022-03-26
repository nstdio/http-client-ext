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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;

class PredicatesTest {
    @Test
    void shouldMatchGivenUri() {
        //given
        URI uri = URI.create("http://example.com");
        HttpRequest r1 = HttpRequest.newBuilder(uri).build();
        HttpRequest r2 = HttpRequest.newBuilder(uri.resolve("/path")).build();

        //when + then
        assertThat(Predicates.uri(uri))
                .accepts(r1)
                .rejects(r2);
    }
}