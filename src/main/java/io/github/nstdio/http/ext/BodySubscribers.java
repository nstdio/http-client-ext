/*
 * Copyright (C) 2022-2025 the original author or authors.
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

import io.github.nstdio.http.ext.spi.JsonMapping;
import io.github.nstdio.http.ext.spi.JsonMappingProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse.BodySubscriber;
import java.util.function.Supplier;

import static java.net.http.HttpResponse.BodySubscribers.mapping;
import static java.net.http.HttpResponse.BodySubscribers.ofInputStream;

@SuppressWarnings("WeakerAccess")
public final class BodySubscribers {
  private BodySubscribers() {
  }

  public static <T> BodySubscriber<Supplier<T>> ofJson(Class<T> targetType) {
    return ofJson(JsonMappingProvider.provider().get(), targetType);
  }

  public static <T> BodySubscriber<Supplier<T>> ofJson(JsonMapping mapping, Class<T> targetType) {
    return mapping(ofInputStream(), in -> () -> {
      try (var stream = in) {
        return mapping.read(stream, targetType);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
  }
}
