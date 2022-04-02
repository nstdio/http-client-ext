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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.nstdio.http.ext.ImmutableResponseInfo.ResponseInfoBuilder;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static io.github.nstdio.http.ext.MetadataSerializationFields.FIELD_NAME_CODE;
import static io.github.nstdio.http.ext.MetadataSerializationFields.FIELD_NAME_HEADERS;
import static io.github.nstdio.http.ext.MetadataSerializationFields.FIELD_NAME_REQUEST;
import static io.github.nstdio.http.ext.MetadataSerializationFields.FIELD_NAME_REQUEST_TIME;
import static io.github.nstdio.http.ext.MetadataSerializationFields.FIELD_NAME_RESPONSE;
import static io.github.nstdio.http.ext.MetadataSerializationFields.FIELD_NAME_RESPONSE_TIME;
import static io.github.nstdio.http.ext.MetadataSerializationFields.FIELD_NAME_VERSION;
import static io.github.nstdio.http.ext.MetadataSerializationFields.FILED_NAME_REQUEST_METHOD;
import static io.github.nstdio.http.ext.MetadataSerializationFields.FILED_NAME_REQUEST_TIMEOUT;
import static io.github.nstdio.http.ext.MetadataSerializationFields.FILED_NAME_REQUEST_URI;
import static java.net.http.HttpRequest.BodyPublishers.noBody;

class GsonMetadataSerializer implements MetadataSerializer {
  private final Gson gson;

  GsonMetadataSerializer() {
    var headers = new HttpHeadersTypeAdapter();
    var request = new HttpRequestTypeAdapter(headers);
    var response = new ResponseInfoTypeAdapter(headers);

    gson = new GsonBuilder()
        .disableHtmlEscaping()
        .registerTypeAdapter(CacheEntryMetadata.class, new CacheEntryMetadataTypeAdapter(request, response))
        .create();
  }

  @Override
  public void write(CacheEntryMetadata metadata, Path path) {
    try (var out = Files.newBufferedWriter(path)) {
      gson.toJson(metadata, out);
    } catch (IOException ignored) {
    }
  }

  @Override
  public CacheEntryMetadata read(Path path) {
    try (var out = Files.newBufferedReader(path)) {
      return gson.fromJson(out, CacheEntryMetadata.class);
    } catch (IOException ignored) {
      return null;
    }
  }

  private static class HttpHeadersTypeAdapter extends TypeAdapter<HttpHeaders> {

    @Override
    public void write(JsonWriter out, HttpHeaders value) throws IOException {
      out.beginObject();

      for (var entry : value.map().entrySet()) {
        out.name(entry.getKey()).beginArray();

        for (String headerValue : entry.getValue()) {
          out.value(headerValue);
        }

        out.endArray();
      }

      out.endObject();
    }

    @Override
    public HttpHeaders read(JsonReader in) throws IOException {
      in.beginObject();
      var builder = new HttpHeadersBuilder();

      while (in.hasNext()) {
        String name = in.nextName();
        List<String> values = new ArrayList<>(1);

        in.beginArray();
        while (in.hasNext()) {
          String value = in.nextString();
          values.add(value);
        }
        in.endArray();

        builder.setTrusted(name, values);
      }

      in.endObject();
      return builder.build();
    }
  }

  @RequiredArgsConstructor
  private static class HttpRequestTypeAdapter extends TypeAdapter<HttpRequest> {
    private final TypeAdapter<HttpHeaders> headersTypeAdapter;

    @Override
    public void write(JsonWriter out, HttpRequest value) throws IOException {
      out.beginObject();

      out.name(FILED_NAME_REQUEST_URI).value(value.uri().toASCIIString());
      out.name(FILED_NAME_REQUEST_METHOD).value(value.method());

      String timeoutString = value.timeout().map(Duration::toString).orElse(null);
      if (timeoutString != null) {
        out.name(FILED_NAME_REQUEST_TIMEOUT).value(timeoutString);
      }
      Integer versionOrd = value.version().map(Enum::ordinal).orElse(null);
      if (versionOrd != null) {
        out.name(FIELD_NAME_VERSION).value(versionOrd);
      }

      out.name(FIELD_NAME_HEADERS);
      headersTypeAdapter.write(out, value.headers());

      out.endObject();
    }

    @Override
    public HttpRequest read(JsonReader in) throws IOException {
      in.beginObject();
      HttpRequest.Builder builder = HttpRequest.newBuilder();

      while (in.hasNext()) {
        switch (in.nextName()) {
          case FILED_NAME_REQUEST_METHOD:
            builder.method(in.nextString(), noBody());
            break;
          case FILED_NAME_REQUEST_TIMEOUT:
            builder.timeout(Duration.parse(in.nextString()));
            break;
          case FIELD_NAME_VERSION:
            int version = in.nextInt();
            builder.version(HttpClient.Version.values()[version]);
            break;
          case FILED_NAME_REQUEST_URI:
            builder.uri(URI.create(in.nextString()));
            break;
          case FIELD_NAME_HEADERS:
            HttpHeaders headers = headersTypeAdapter.read(in);
            headers.map().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
            break;
        }
      }

      in.endObject();

      return builder.build();
    }
  }

  @RequiredArgsConstructor
  private static class ResponseInfoTypeAdapter extends TypeAdapter<ResponseInfo> {
    private final TypeAdapter<HttpHeaders> headersTypeAdapter;

    @Override
    public void write(JsonWriter out, ResponseInfo value) throws IOException {
      out.beginObject();

      out.name(FIELD_NAME_CODE).value(value.statusCode());
      out.name(FIELD_NAME_VERSION).value(value.version().ordinal());

      out.name(FIELD_NAME_HEADERS);
      headersTypeAdapter.write(out, value.headers());

      out.endObject();
    }

    @Override
    public ResponseInfo read(JsonReader in) throws IOException {
      in.beginObject();
      ResponseInfoBuilder builder = ImmutableResponseInfo.builder();
      String fieldName;

      while (in.hasNext()) {
        fieldName = in.nextName();

        switch (fieldName) {
          case FIELD_NAME_CODE:
            builder.statusCode(in.nextInt());
            break;
          case FIELD_NAME_VERSION:
            int version = in.nextInt();
            builder.version(HttpClient.Version.values()[version]);
            break;
          case FIELD_NAME_HEADERS:
            HttpHeaders headers = headersTypeAdapter.read(in);
            builder.headers(headers);
            break;
        }
      }

      in.endObject();

      return builder.build();
    }
  }

  @RequiredArgsConstructor
  private static class CacheEntryMetadataTypeAdapter extends TypeAdapter<CacheEntryMetadata> {
    private final TypeAdapter<HttpRequest> requestTypeAdapter;
    private final TypeAdapter<ResponseInfo> responseTypeAdapter;

    @Override
    public void write(JsonWriter out, CacheEntryMetadata value) throws IOException {
      out.beginObject();

      out.name(FIELD_NAME_REQUEST_TIME).value(value.requestTime());
      out.name(FIELD_NAME_RESPONSE_TIME).value(value.responseTime());

      out.name(FIELD_NAME_REQUEST);
      requestTypeAdapter.write(out, value.request());

      out.name(FIELD_NAME_RESPONSE);
      responseTypeAdapter.write(out, value.response());

      out.endObject();
    }

    @Override
    public CacheEntryMetadata read(JsonReader in) throws IOException {
      in.beginObject();

      long requestTime = -1;
      long responseTime = -1;
      HttpRequest request = null;
      ResponseInfo response = null;

      while (in.hasNext()) {
        switch (in.nextName()) {
          case FIELD_NAME_REQUEST_TIME:
            requestTime = in.nextLong();
            break;
          case FIELD_NAME_RESPONSE_TIME:
            responseTime = in.nextLong();
            break;
          case FIELD_NAME_REQUEST:
            request = requestTypeAdapter.read(in);
            break;
          case FIELD_NAME_RESPONSE:
            response = responseTypeAdapter.read(in);
            break;
        }
      }

      in.endObject();
      return CacheEntryMetadata.of(requestTime, responseTime, response, request, Clock.systemUTC());
    }
  }
}
