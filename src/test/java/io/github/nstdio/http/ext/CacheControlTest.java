package io.github.nstdio.http.ext;

import static io.github.nstdio.http.ext.Headers.HEADER_CACHE_CONTROL;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

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
            "max-age=32,max-stale=64,min-fresh=128",
            "max-age=\"32\" ,max-stale=\"64\",min-fresh=\"128\"",
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
        assertThat(actual).hasToString("max-age=32, max-stale=64, min-fresh=128");
    }

    @Test
    void shouldParseAndRoundRobin() {
        //given
        var minFresh = 5;
        var maxStale = 6;
        var maxAge = 7;
        var cc = CacheControl
                .builder()
                .minFresh(minFresh)
                .maxStale(maxStale)
                .maxAge(maxAge)
                .noCache()
                .noTransform()
                .onlyIfCached()
                .noStore()
                .mustRevalidate()
                .build();
        var expected = String.format("no-cache, no-store, must-revalidate, no-transform, only-if-cached, max-age=%d, max-stale=%d, min-fresh=%d", maxAge, maxStale, minFresh);

        //when + then
        assertThat(cc).hasToString(expected);
        assertThat(CacheControl.parse(expected))
                .usingRecursiveComparison()
                .isEqualTo(cc);
    }
}