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

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

class BodyHandlersTest {

    @Nested
    class OfJsonTest {
        private final HttpClient client = HttpClient.newHttpClient();

        @Test
        void shouldProperlyReadJson() {
            //given
            var request = HttpRequest.newBuilder(URI.create("https://httpbin.org/get")).build();
            TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
            };

            //when
            var body1 = client.sendAsync(request, BodyHandlers.ofJson(typeReference))
                    .thenApply(HttpResponse::body)
                    .join();
            var body2 = client.sendAsync(request, BodyHandlers.ofJson(Object.class))
                    .thenApply(HttpResponse::body)
                    .join();

            //then
            assertThat(body1).isNotEmpty();
            assertThat(body2).isNotNull();
        }
    }
}