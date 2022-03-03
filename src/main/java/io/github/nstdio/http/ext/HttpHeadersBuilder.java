/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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