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

class MetadataSerializationFields {
  static final String FIELD_NAME_VERSION = "version";
  static final String FIELD_NAME_REQUEST_TIME = "requestTime";
  static final String FIELD_NAME_RESPONSE_TIME = "responseTime";
  static final String FIELD_NAME_REQUEST = "request";
  static final String FIELD_NAME_RESPONSE = "response";
  static final String FIELD_NAME_HEADERS = "headers";
  static final String FIELD_NAME_CODE = "code";

  static final String FILED_NAME_REQUEST_METHOD = "method";
  static final String FILED_NAME_REQUEST_TIMEOUT = "timeout";
  static final String FILED_NAME_REQUEST_URI = "uri";

  private MetadataSerializationFields() {
  }
}
