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

import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse.ResponseInfo;

@Builder(builderClassName = "ResponseInfoBuilder")
@RequiredArgsConstructor
class ResponseInfoImpl implements ResponseInfo {
    private final int statusCode;
    private final HttpHeaders headers;
    private final Version version;

    static ResponseInfoBuilder toBuilder(ResponseInfo info) {
        return builder()
                .version(info.version())
                .headers(info.headers())
                .statusCode(info.statusCode());
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public Version version() {
        return version;
    }

    static class ResponseInfoBuilder {}
}
