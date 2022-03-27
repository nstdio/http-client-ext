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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

import static io.github.nstdio.http.ext.Headers.HEADER_CACHE_CONTROL;
import static org.assertj.core.api.Assertions.assertThat;

class CacheControlTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "max-age=92233720368547758070,max-stale=92233720368547758070,min-fresh=92233720368547758070",
      "max-age= ,max-stale=,min-fresh=abc",
  })
  void shouldNotFailWhenLongOverflow(String value) {
    //given
    var httpHeaders = HttpHeaders.of(Map.of(HEADER_CACHE_CONTROL, List.of(value)), (s, s2) -> true);

    //when
    var actual = CacheControl.of(httpHeaders);

    //then
    assertThat(actual).hasToString("");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "max-age=32,max-stale=64,min-fresh=128,stale-if-error=256,stale-while-revalidate=512",
      "max-age=\"32\" ,max-stale=\"64\",min-fresh=\"128\",stale-if-error=\"256\",stale-while-revalidate=\"512\"",
  })
  void shouldParseValues(String value) {
    //given
    var httpHeaders = HttpHeaders.of(Map.of(HEADER_CACHE_CONTROL, List.of(value)), (s, s2) -> true);

    //when
    var actual = CacheControl.of(httpHeaders);

    //then
    assertThat(actual.maxAge()).isEqualTo(32);
    assertThat(actual.maxStale()).isEqualTo(64);
    assertThat(actual.minFresh()).isEqualTo(128);
    assertThat(actual.staleIfError()).isEqualTo(256);
    assertThat(actual.staleWhileRevalidate()).isEqualTo(512);
    assertThat(actual).hasToString("max-age=32, max-stale=64, min-fresh=128, stale-if-error=256, stale-while-revalidate=512");
  }

  @Test
  void shouldParseAndRoundRobin() {
    //given
    var minFresh = 5;
    var maxStale = 6;
    var maxAge = 7;
    var staleIfError = 8;
    var staleWhileRevalidate = 9;
    var cc = CacheControl
        .builder()
        .minFresh(minFresh)
        .maxStale(maxStale)
        .maxAge(maxAge)
        .staleIfError(staleIfError)
        .staleWhileRevalidate(staleWhileRevalidate)
        .noCache()
        .noTransform()
        .onlyIfCached()
        .noStore()
        .mustRevalidate()
        .immutable()
        .build();
    var expected = String.format("no-cache, no-store, must-revalidate, no-transform, immutable, only-if-cached, max-age=%d, max-stale=%d, min-fresh=%d, stale-if-error=%d, stale-while-revalidate=%d", maxAge, maxStale, minFresh, staleIfError, staleWhileRevalidate);

    //when + then
    assertThat(cc).hasToString(expected);
    assertThat(CacheControl.parse(expected))
        .usingRecursiveComparison()
        .isEqualTo(cc);
  }
}