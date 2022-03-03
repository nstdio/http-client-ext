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

import static io.github.nstdio.http.ext.Assertions.assertThat;
import static io.github.nstdio.http.ext.Headers.toRFC1123;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.ResponseInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class CacheEntryMetadataTest {

    @Test
    void varyHeaders() {
        var headers = Map.of(
                "Vary", "Accept, Accept-Encoding, User-Agent"
        );
        var r = HttpRequest.newBuilder(URI.create("https://example.com"))
                .headers("Accept", "text/plain",
                        "Accept-Encoding", "gzip",
                        "User-Agent", "Java/11",
                        "Accept-Language", "en-EN")
                .build();


        var m = new CacheEntryMetadata(0, 0, Helpers.responseInfo(headers), r, Clock.systemDefaultZone());

        //when
        var actual = m.varyHeaders();

        //then
        assertThat(actual)
                .hasHeaderWithValues("Accept", "text/plain")
                .hasHeaderWithValues("Accept-Encoding", "gzip")
                .hasHeaderWithValues("User-Agent", "Java/11");
    }

    @Test
    void shouldGenerateHeuristicExpirationWarning() throws Exception {
        //given
        var tickDuration = Duration.ofSeconds(1);
        var baseInstant = Instant.ofEpochSecond(0);
        var clock = new FixedRateTickClock(baseInstant, ZoneOffset.UTC, tickDuration);
        // Creating Last-Modified just about to exceed 24 hour limit
        var lastModified = baseInstant.minus(241, HOURS);
        var expectedExpirationTime = baseInstant.plus(2, DAYS);

        var info = Helpers.responseInfo(Map.of(
                "Last-Modified", toRFC1123(lastModified),
                "Date", toRFC1123(Instant.ofEpochSecond(0))
        ));
        var request1 = HttpRequest.newBuilder().uri(URI.create("https://www.example.com")).build();
        var request2 = HttpRequest.newBuilder().uri(URI.create("https://www.example.com/path?query")).build();

        var metadata1 = new CacheEntryMetadata(0, 1000, info, request1, clock);
        var metadata2 = new CacheEntryMetadata(0, 1000, info, request2, clock);

        tickUntil(clock, expectedExpirationTime);
        var executor = Executors.newFixedThreadPool(12);

        //when
        for (int i = 0; i < 1 << 16; i++) {
            executor.submit(metadata1::updateWarnings);
        }

        executor.shutdown();
        //noinspection ResultOfMethodCallIgnored
        executor.awaitTermination(3, TimeUnit.SECONDS);

        //then
        assertTrue(metadata2.maxAge() < 0);
        assertFalse(metadata2.isApplicable());
        assertThat(metadata1.response().headers())
                .hasHeaderWithOnlyValue("Warning", "113 - \"Heuristic Expiration\"");
    }

    private void tickUntil(FixedRateTickClock clock, Instant expectedExpirationTime) {
        while (clock.instant().isBefore(expectedExpirationTime)) {}
    }

    /**
     * https://httpwg.org/specs/rfc7234.html#rfc.section.5.2.2.1
     */
    @Test
    void shouldRespectMustRevalidateResponseDirective() {
        //given
        var baseClock = Clock.systemUTC();
        var dateHeader = baseClock.instant();
        var clock = FixedRateTickClock.of(baseClock, Duration.ofSeconds(1));
        var info = Helpers.responseInfo(Map.of(
                "Cache-Control", "max-age=1,must-revalidate",
                "Date", toRFC1123(dateHeader)
        ));
        var request = HttpRequest.newBuilder().uri(URI.create("https://www.example.com")).build();
        var requestTimeMs = dateHeader.minusMillis(50).toEpochMilli();
        var responseTimeMs = dateHeader.plusMillis(50).toEpochMilli();

        var metadata = new CacheEntryMetadata(requestTimeMs, responseTimeMs, info, request, clock);

        //when + then
        assertFalse(metadata.isFresh(CacheControl.parse("max-stale=1")));
    }

    @Test
    @Disabled("https://httpwg.org/specs/rfc7234.html#rfc.section.4.2.4")
    void shouldAddStaleResponseWarning() {
    }

    @Nested
    class Freshness {
        private final URI uri = URI.create("https://www.example.com");

        @Test
        void shouldBeFresh() {
            //given
            long requestTimeMs = 100;
            long responseTimeMs = 350;
            long serverDate = responseTimeMs - 50;

            Clock clock = Clock.fixed(Instant.ofEpochMilli(responseTimeMs + 5), ZoneId.systemDefault());

            ResponseInfo info = Helpers.responseInfo(Map.of(
                    "Cache-Control", "max-age=1",
                    "Date", toRFC1123(Instant.ofEpochMilli(serverDate))
            ));
            HttpRequest request = HttpRequest.newBuilder().uri(uri).build();

            CacheEntryMetadata e = new CacheEntryMetadata(requestTimeMs, responseTimeMs, info, request, clock);
            //when + then
            assertTrue(e.isFresh(CacheControl.builder().build()));
        }
    }
}