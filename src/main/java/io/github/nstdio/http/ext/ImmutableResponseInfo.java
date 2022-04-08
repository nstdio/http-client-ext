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

import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse.ResponseInfo;

class ImmutableResponseInfo implements ResponseInfo {
  private final int statusCode;
  private final HttpHeaders headers;
  private final Version version;

  private ImmutableResponseInfo(int statusCode, HttpHeaders headers, Version version) {
    this.statusCode = statusCode;
    this.headers = headers;
    this.version = version;
  }

  static ResponseInfoBuilder toBuilder(ResponseInfo info) {
    return builder()
        .version(info.version())
        .headers(info.headers())
        .statusCode(info.statusCode());
  }

  public static ResponseInfoBuilder builder() {
    return new ResponseInfoBuilder();
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

  static class ResponseInfoBuilder {
    private int statusCode;
    private HttpHeaders headers;
    private Version version;

    ResponseInfoBuilder() {
    }

    public ResponseInfoBuilder statusCode(int statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    public ResponseInfoBuilder headers(HttpHeaders headers) {
      this.headers = headers;
      return this;
    }

    public ResponseInfoBuilder version(Version version) {
      this.version = version;
      return this;
    }

    public ImmutableResponseInfo build() {
      return new ImmutableResponseInfo(statusCode, headers, version);
    }
  }
}
