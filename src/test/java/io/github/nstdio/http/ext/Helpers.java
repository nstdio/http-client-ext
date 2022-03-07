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

import static java.util.Map.entry;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class Helpers {
    private Helpers() {
    }

    static HttpResponse.ResponseInfo responseInfo(Map<String, String> headers) {
        return new HttpResponse.ResponseInfo() {
            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public HttpHeaders headers() {
                return headers.entrySet()
                        .stream()
                        .map(e -> entry(e.getKey(), List.of(e.getValue())))
                        .collect(collectingAndThen(toMap(Map.Entry::getKey, Map.Entry::getValue), map -> HttpHeaders.of(map, (s, s2) -> true)));
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }

    static List<ByteBuffer> toBuffers(byte[] bytes, boolean checkResult) {
        List<ByteBuffer> ret = new ArrayList<>();
        int start = 0;
        int stop = bytes.length + 1;
        int end = Math.max(1, RandomUtils.nextInt(start, stop));

        while (end != stop) {
            byte[] copy = Arrays.copyOfRange(bytes, start, end);

            ret.add(ByteBuffer.wrap(copy));

            start = end;
            end = RandomUtils.nextInt(start + 1, stop);
        }

        if (checkResult) {
            int i = 0;
            for (ByteBuffer b : ret) {
                while (b.hasRemaining()) {
                    assertEquals(bytes[i++], b.get());
                }

                b.flip();
            }
        }

        return ret;
    }

    static String randomString(int min, int max) {
        int count = RandomUtils.nextInt(min, max + 1);

        return RandomStringUtils.randomAlphabetic(count);
    }
}
