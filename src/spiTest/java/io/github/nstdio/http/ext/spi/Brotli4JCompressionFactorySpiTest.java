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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class Brotli4JCompressionFactorySpiTest {

  @Test
  void shouldDecompress() throws IOException {
    //given
    InputStream inputStream = URI.create("https://httpbin.org/brotli").toURL().openStream();
    Brotli4JCompressionFactory factory = new Brotli4JCompressionFactory();

    //when
    List<String> supported = factory.supported();
    InputStream actual = factory.decompressing(inputStream, "br");
    String actualAsString = IOUtils.toString(actual, UTF_8);

    //then
    isJson().matches(actualAsString);
    assertThat(supported).containsExactly("br");
  }
}