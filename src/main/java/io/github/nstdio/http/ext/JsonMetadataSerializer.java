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

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.github.nstdio.http.ext.ImmutableResponseInfo.ResponseInfoBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

class JsonMetadataSerializer implements MetadataSerializer {

    private final ObjectWriter writer;
    private final ObjectReader reader;

    JsonMetadataSerializer() {
        ObjectMapper mapper = createMapper();
        writer = mapper.writerFor(CacheEntryMetadata.class);
        reader = mapper.readerFor(CacheEntryMetadata.class);
    }

    private ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule("JsonMetadataSerializer");

        simpleModule.addSerializer(CacheEntryMetadata.class, new CacheMetadataSerializer());
        simpleModule.addDeserializer(CacheEntryMetadata.class, new CacheMetadataDeserializer());

        simpleModule.addSerializer(HttpRequest.class, new HttpRequestSerializer());
        simpleModule.addDeserializer(HttpRequest.class, new HttpRequestDeserializer());

        simpleModule.addSerializer(ResponseInfo.class, new ResponseInfoSerializer());
        simpleModule.addDeserializer(ResponseInfo.class, new ResponseInfoDeserializer());

        simpleModule.addSerializer(HttpHeaders.class, new HttpHeadersSerializer());
        simpleModule.addDeserializer(HttpHeaders.class, new HttpHeadersDeserializer());

        mapper.registerModule(simpleModule);
        return mapper;
    }

    @Override
    public void write(CacheEntryMetadata metadata, Path path) {
        try (var out = new GZIPOutputStream(Files.newOutputStream(path, WRITE, CREATE))) {
            writer.writeValue(out, metadata);
        } catch (IOException ignore) {
            // noop
        }
    }

    @Override
    public CacheEntryMetadata read(Path path) {
        try (var in = new GZIPInputStream(Files.newInputStream(path, READ))) {
            return reader.readValue(in);
        } catch (IOException e) {
            return null;
        }
    }

    static class CacheMetadataSerializer extends StdSerializer<CacheEntryMetadata> {
        CacheMetadataSerializer() {
            super(CacheEntryMetadata.class);
        }

        @Override
        public void serialize(CacheEntryMetadata value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();

            gen.writeNumberField("requestTime", value.requestTime());
            gen.writeNumberField("responseTime", value.responseTime());
            gen.writeObjectField("request", value.request());
            gen.writeObjectField("response", value.response());

            gen.writeEndObject();
        }
    }

    static class CacheMetadataDeserializer extends StdDeserializer<CacheEntryMetadata> {
        private final JavaType requestType = TypeFactory.defaultInstance().constructType(HttpRequest.class);
        private final JavaType responseType = TypeFactory.defaultInstance().constructType(ResponseInfo.class);

        CacheMetadataDeserializer() {
            super(CacheEntryMetadata.class);
        }

        @Override
        public CacheEntryMetadata deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            long requestTime = -1;
            long responseTime = -1;
            HttpRequest request = null;
            ResponseInfo response = null;
            String fieldName;

            while ((fieldName = p.nextFieldName()) != null) {
                switch (fieldName) {
                    case "requestTime":
                        requestTime = p.nextLongValue(-1);
                        break;
                    case "responseTime":
                        responseTime = p.nextLongValue(-1);
                        break;
                    case "request":
                        p.nextToken();
                        request = (HttpRequest) ctxt.findRootValueDeserializer(requestType).deserialize(p, ctxt);
                        break;
                    case "response":
                        p.nextToken();
                        response = (ResponseInfo) ctxt.findRootValueDeserializer(responseType).deserialize(p, ctxt);
                        break;
                }
            }

            return CacheEntryMetadata.of(requestTime, responseTime, response, request, Clock.systemUTC());
        }
    }

    static class HttpRequestSerializer extends StdSerializer<HttpRequest> {
        HttpRequestSerializer() {
            super(HttpRequest.class);
        }

        @Override
        public void serialize(HttpRequest value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("method", value.method());

            String timeoutString = value.timeout().map(Duration::toString).orElse(null);
            if (timeoutString != null) {
                gen.writeStringField("timeout", timeoutString);
            }

            gen.writeStringField("uri", value.uri().toASCIIString());
            Integer versionOrd = value.version().map(Enum::ordinal).orElse(null);
            if (versionOrd != null) {
                gen.writeNumberField("version", versionOrd);
            }

            gen.writeObjectField("headers", value.headers());

            gen.writeEndObject();
        }
    }

    static class HttpRequestDeserializer extends StdDeserializer<HttpRequest> {
        private final JavaType headersType = TypeFactory.defaultInstance().constructType(HttpHeaders.class);

        HttpRequestDeserializer() {
            super(HttpRequest.class);
        }

        @Override
        public HttpRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            Builder builder = HttpRequest.newBuilder();
            String fieldName;

            while ((fieldName = p.nextFieldName()) != null) {
                switch (fieldName) {
                    case "method":
                        builder.method(p.nextTextValue(), noBody());
                        break;
                    case "timeout":
                        String timeout = p.nextTextValue();
                        builder.timeout(Duration.parse(timeout));
                        break;
                    case "version":
                        int version = p.nextIntValue(-1);
                        builder.version(HttpClient.Version.values()[version]);
                        break;
                    case "uri":
                        builder.uri(URI.create(p.nextTextValue()));
                        break;
                    case "headers":
                        p.nextToken();
                        HttpHeaders headers = (HttpHeaders) ctxt.findRootValueDeserializer(headersType).deserialize(p, ctxt);
                        headers.map().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
                        break;
                }
            }

            return builder.build();
        }
    }

    static class ResponseInfoSerializer extends StdSerializer<ResponseInfo> {
        ResponseInfoSerializer() {
            super(ResponseInfo.class);
        }

        @Override
        public void serialize(ResponseInfo value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();

            gen.writeNumberField("code", value.statusCode());
            gen.writeNumberField("version", value.version().ordinal());
            gen.writeObjectField("headers", value.headers());

            gen.writeEndObject();
        }
    }

    static class ResponseInfoDeserializer extends StdDeserializer<ResponseInfo> {
        private final JavaType headersType = TypeFactory.defaultInstance().constructType(HttpHeaders.class);
        private final HttpClient.Version[] values = HttpClient.Version.values();

        ResponseInfoDeserializer() {
            super(ResponseInfo.class);
        }

        @Override
        public ResponseInfo deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            ResponseInfoBuilder builder = ImmutableResponseInfo.builder();

            String fieldName;
            while ((fieldName = p.nextFieldName()) != null) {
                switch (fieldName) {
                    case "code":
                        builder.statusCode(p.nextIntValue(-1));
                        break;
                    case "version":
                        builder.version(values[p.nextIntValue(-1)]);
                        break;
                    case "headers":
                        JsonDeserializer<Object> headersDeserializer = ctxt.findRootValueDeserializer(headersType);
                        p.nextToken();
                        HttpHeaders headers = (HttpHeaders) headersDeserializer.deserialize(p, ctxt);
                        builder.headers(headers);
                        break;
                }
            }

            return builder.build();
        }
    }

    static class HttpHeadersSerializer extends StdSerializer<HttpHeaders> {
        HttpHeadersSerializer() {
            super(HttpHeaders.class);
        }

        @Override
        public void serialize(HttpHeaders value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();

            Map<String, List<String>> map = value.map();
            for (var entry : map.entrySet()) {
                gen.writeFieldName(entry.getKey());

                String[] values = entry.getValue().toArray(new String[0]);
                gen.writeArray(values, 0, values.length);
            }

            gen.writeEndObject();
        }
    }

    static class HttpHeadersDeserializer extends StdDeserializer<HttpHeaders> {
        private final MapType mapType;

        HttpHeadersDeserializer() {
            super(HttpHeaders.class);
            TypeFactory typeFactory = TypeFactory.defaultInstance();

            JavaType keyType = typeFactory.constructType(String.class);
            CollectionType valueType = typeFactory.constructCollectionType(ArrayList.class, String.class);
            this.mapType = typeFactory.constructMapType(HashMap.class, keyType, valueType);
        }

        @Override
        public HttpHeaders deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            @SuppressWarnings("unchecked")
            var deserialize = (Map<String, List<String>>) ctxt.findRootValueDeserializer(mapType).deserialize(p, ctxt);

            return HttpHeaders.of(deserialize, Headers.ALLOW_ALL);
        }
    }
}