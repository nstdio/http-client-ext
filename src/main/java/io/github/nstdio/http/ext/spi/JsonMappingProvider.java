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

package io.github.nstdio.http.ext.spi;

import static java.util.Objects.requireNonNull;

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
    return JsonMappingProviders.provider();
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
    return JsonMappingProviders.provider(name);
  }

  /**
   * Adds {@code provider}.
   *
   * @param provider The provider to add.
   */
  static void addProvider(JsonMappingProvider provider) {
    JsonMappingProviders.addProvider(requireNonNull(provider, "provider cannot be null"));
  }

  /**
   * Removes provider with name {@code name}.
   *
   * @param name The provider name to remove.
   */
  static void removeProvider(String name) {
    JsonMappingProviders.removeProvider(requireNonNull(name, "name cannot be null"));
  }

  /**
   * Returns an instance of {@code JsonMapping}.
   *
   * @return The {@code JsonMapping} instance.
   */
  JsonMapping get();
}
