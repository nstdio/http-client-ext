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

import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class HttpHeadersBuilder {
    private final TreeMap<String, List<String>> headersMap;

    HttpHeadersBuilder() {
        headersMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    HttpHeadersBuilder(HttpHeaders headers) {
        this();
        copyTo(this, headers.map());
    }

    private void copyTo(HttpHeadersBuilder builder, Map<String, List<String>> source) {
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            List<String> valuesCopy = new ArrayList<>(entry.getValue());
            builder.headersMap.put(entry.getKey(), valuesCopy);
        }
    }

    HttpHeadersBuilder addHeader(String name, String value) {
        headersMap.computeIfAbsent(name, k -> new ArrayList<>(1))
                .add(value);
        return this;
    }

    HttpHeadersBuilder addHeader(String name, List<String> values) {
        if (!values.isEmpty()) {
            headersMap.computeIfAbsent(name, k -> new ArrayList<>(values.size()))
                    .addAll(values);
        }
        return this;
    }

    HttpHeadersBuilder setHeader(String name, String value) {
        List<String> values = new ArrayList<>(1);
        values.add(value);
        headersMap.put(name, values);

        return this;
    }

    HttpHeadersBuilder setHeader(String name, List<String> value) {
        List<String> values = new ArrayList<>(value);
        headersMap.put(name, values);

        return this;
    }

    HttpHeadersBuilder removeHeader(String name, String value) {
        List<String> values = headersMap.get(name);
        if (value != null) {
            values.remove(value);
            if (values.isEmpty()) {
                headersMap.remove(name);
            }
        }
        return this;
    }

    HttpHeaders build() {
        return HttpHeaders.of(headersMap, (s, s2) -> true);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(" { ");
        sb.append(headersMap);
        sb.append(" }");
        return sb.toString();
    }
}