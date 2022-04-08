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

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

/**
 * The SPI to for {@linkplain JsonMapping}.
 */
public interface JsonMappingProvider {
  /**
   * Finds the first available {@code JsonMappingProvider}.
   *
   * @return The {@code JsonMappingProvider}.
   *
   * @throws JsonMappingProviderNotFoundException When cannot find any provider.
   */
  static JsonMappingProvider provider() {
    return loader()
        .findFirst()
        .or(JsonMappingProvider::defaultProvider)
        .orElseThrow(() -> new JsonMappingProviderNotFoundException("Cannot find any JsonMappingProvider."));
  }

  /**
   * Finds the {@code JsonMappingProvider} with given name.
   *
   * @param name The provider fully qualified class name.
   *
   * @return The {@code JsonMappingProvider} with given name.
   *
   * @throws JsonMappingProviderNotFoundException When requested provider is not found.
   */
  static JsonMappingProvider provider(String name) {
    return loader()
        .stream()
        .filter(provider -> provider.type().getName().equals(name))
        .findFirst()
        .map(Provider::get)
        .or(() -> defaultProvider().filter(provider -> provider.getClass().getName().equals(name)))
        .orElseThrow(() -> new JsonMappingProviderNotFoundException("JsonMappingProvider not found: " + name));
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

  /**
   * Returns an instance of {@code JsonMapping}.
   *
   * @return The {@code JsonMapping} instance.
   */
  JsonMapping get();
}
