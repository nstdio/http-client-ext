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

import io.github.nstdio.http.ext.jupiter.DisabledIfOnClasspath;
import io.github.nstdio.http.ext.jupiter.EnabledIfOnClasspath;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.github.nstdio.http.ext.OptionalDependencies.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;

class JsonMappingProviderSpiTest {

  @Test
  void shouldGetDefaultProviderByName() {
    assumeThat(ALL_JSON)
        .anyMatch(Classpath::isPresent);

    //given
    var providerName = CompositeJsonMappingProvider.class.getName();

    //when + then
    JsonMappingProvider provider = JsonMappingProvider.provider(providerName);

    //then
    assertThat(provider).isExactlyInstanceOf(CompositeJsonMappingProvider.class);
  }

  @Nested
  @EnabledIfOnClasspath(GSON)
  @DisabledIfOnClasspath(JACKSON)
  class GsonPresentJacksonMissingTest {

    @Test
    void shouldLoadGsonIfJacksonMissing() {
      //when
      JsonMappingProvider provider = JsonMappingProvider.provider();
      JsonMapping jsonMapping = provider.get();

      //then
      assertThat(provider).isExactlyInstanceOf(CompositeJsonMappingProvider.class);
      assertThat(jsonMapping).isExactlyInstanceOf(GsonJsonMapping.class);
    }
  }

  @Nested
  @EnabledIfOnClasspath(value = JACKSON)
  @DisabledIfOnClasspath(value = GSON)
  class JacksonPresentGsonMissingTest {
    @Test
    void shouldLoadDefaultJackson() {
      //when
      JsonMappingProvider provider = JsonMappingProvider.provider();
      JsonMapping jsonMapping = provider.get();

      //then
      assertThat(provider).isExactlyInstanceOf(CompositeJsonMappingProvider.class);
      assertThat(jsonMapping).isExactlyInstanceOf(JacksonJsonMapping.class);
    }
  }

  @Nested
  @DisabledIfOnClasspath({JACKSON, GSON})
  class AllMissingTest {
    @Test
    void shouldThrowWhenNothingIsPresent() {
      //when + then
      assertThatExceptionOfType(JsonMappingProviderNotFoundException.class)
          .isThrownBy(JsonMappingProvider::provider);
    }

    @Test
    void shouldThrowWhenNothingIsPresentAndRequestedByName() {
      //given
      var providerName = CompositeJsonMappingProvider.class.getName();

      //when + then
      assertThatExceptionOfType(JsonMappingProviderNotFoundException.class)
          .isThrownBy(() -> JsonMappingProvider.provider(providerName));
    }
  }

  @Nested
  @EnabledIfOnClasspath({JACKSON, GSON})
  class AllPresentTest {
    @Test
    void shouldLoadDefaultJackson() {
      //when
      JsonMappingProvider provider = JsonMappingProvider.provider();
      JsonMapping jsonMapping = provider.get();

      //then
      assertThat(provider).isExactlyInstanceOf(CompositeJsonMappingProvider.class);
      assertThat(jsonMapping).isExactlyInstanceOf(JacksonJsonMapping.class);
    }
  }
}
