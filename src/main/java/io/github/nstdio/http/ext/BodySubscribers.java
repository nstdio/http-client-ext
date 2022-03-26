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

import static java.net.http.HttpResponse.BodySubscribers.mapping;
import static java.net.http.HttpResponse.BodySubscribers.ofByteArray;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse.BodySubscriber;

@SuppressWarnings("WeakerAccess")
public final class BodySubscribers {
    private static final TypeFactory TF = TypeFactory.defaultInstance();

    private BodySubscribers() {
    }

    public static <T> BodySubscriber<T> ofJson(Class<T> targetType) {
        return ofJson(TF.constructType(targetType));
    }

    public static <T> BodySubscriber<T> ofJson(TypeReference<T> targetType) {
        return ofJson(TF.constructType(targetType));
    }

    public static <T> BodySubscriber<T> ofJson(ObjectMapper objectMapper, Class<T> targetType) {
        return ofJson(objectMapper, objectMapper.getTypeFactory().constructType(targetType));
    }

    public static <T> BodySubscriber<T> ofJson(ObjectMapper mapper, TypeReference<T> targetType) {
        return ofJson(mapper, mapper.getTypeFactory().constructType(targetType));
    }

    private static <T> BodySubscriber<T> ofJson(JavaType targetType) {
        return ofJson(ObjectMapperHolder.INSTANCE, targetType);
    }

    private static <T> BodySubscriber<T> ofJson(ObjectMapper objectMapper, JavaType targetType) {
        return mapping(ofByteArray(), bytes -> {
            try {
                return objectMapper.readValue(bytes, targetType);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static class ObjectMapperHolder {
        private static final ObjectMapper INSTANCE = new ObjectMapper();
    }
}
