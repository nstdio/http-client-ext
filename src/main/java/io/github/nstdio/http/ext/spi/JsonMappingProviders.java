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

package io.github.nstdio.http.ext.spi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

class JsonMappingProviders {
  private static final List<Provider<JsonMappingProvider>> userProvided = new CopyOnWriteArrayList<>();

  static void addProvider(JsonMappingProvider provider) {
    userProvided.add(0, new InitializedProvider(provider));
  }

  static void removeProvider(String name) {
    userProvided.removeIf(p -> p.type().getName().equals(name));
  }

  static void clear() {
    userProvided.clear();
  }

  static JsonMappingProvider provider() {
    return providerStream()
        .findFirst()
        .map(Provider::get)
        .or(JsonMappingProviders::defaultProvider)
        .orElseThrow(() -> new JsonMappingProviderNotFoundException("Cannot find any JsonMappingProvider."));
  }

  static JsonMappingProvider provider(String name) {
    return providerStream()
        .filter(provider -> provider.type().getName().equals(name))
        .findFirst()
        .map(Provider::get)
        .or(() -> defaultProvider().filter(provider -> provider.getClass().getName().equals(name)))
        .orElseThrow(() -> new JsonMappingProviderNotFoundException("JsonMappingProvider not found: " + name));
  }

  private static Stream<Provider<JsonMappingProvider>> providerStream() {
    return Stream.concat(userProvided.stream(), loader().stream());
  }

  private static Optional<JsonMappingProvider> defaultProvider() {
    if (CompositeJsonMappingProvider.hasAnyImplementation()) {
      return Optional.of(DefaultProviderHolder.DEFAULT_PROVIDER);
    } else {
      return Optional.empty();
    }
  }

  private static ServiceLoader<JsonMappingProvider> loader() {
    return ServiceLoader.load(JsonMappingProvider.class);
  }

  private static class InitializedProvider implements Provider<JsonMappingProvider> {
    private final JsonMappingProvider mappingProvider;

    private InitializedProvider(JsonMappingProvider mappingProvider) {
      this.mappingProvider = Objects.requireNonNull(mappingProvider);
    }

    @Override
    public Class<? extends JsonMappingProvider> type() {
      return mappingProvider.getClass();
    }

    @Override
    public JsonMappingProvider get() {
      return mappingProvider;
    }
  }

  private static final class DefaultProviderHolder {
    private static final CompositeJsonMappingProvider DEFAULT_PROVIDER = new CompositeJsonMappingProvider();
  }
}
