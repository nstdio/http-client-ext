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

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

class FixedRateTickClockTest {

    @RepeatedTest(512)
    void shouldTickAtFixedRate() {
        //given
        var baseInstant = Instant.ofEpochSecond(0);
        var tick = Duration.ofSeconds(5);
        var clock = new FixedRateTickClock(baseInstant, ZoneOffset.UTC, tick);

        //when
        Instant[] actual = IntStream.range(0, 32)
                .mapToObj(i -> clock.instant())
                .toArray(Instant[]::new);

        //then
        assertEvenlyDistributed(actual, tick);
    }

    private void assertEvenlyDistributed(Instant[] actual, Duration tick) {
        for (int i = 0, n = actual.length; i < n; i++) {
            for (int j = 0; j < n; j++) {
                var expectedTick = tick.multipliedBy(j - i);
                var actualDuration = Duration.between(actual[i], actual[j]);

                assertThat(actualDuration).isEqualTo(expectedTick);
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ThreadSafetyTest {
        private final Instant baseInstant = Instant.ofEpochSecond(0);
        private final Duration tick = Duration.ofSeconds(1);
        private final FixedRateTickClock clock = new FixedRateTickClock(baseInstant, ZoneOffset.UTC, tick);

        private final int nTasks = 64;
        private final int nThreads = 12;
        private final ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        @AfterAll
        void tearDown() {
            executor.shutdown();
        }

        @RepeatedTest(512)
        void shouldBeSafe() {
            //when
            var futures = IntStream.range(0, nTasks)
                    .mapToObj(i -> executor.submit(clock::instant))
                    .collect(toList());

            var actual = futures.stream()
                    .map(f -> {
                        try {
                            return f.get(1, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .sorted()
                    .collect(toCollection(LinkedHashSet::new));

            //then

            // Implicitly checking clock to not return same instant more than once
            assertThat(actual).hasSize(nTasks);

            assertEvenlyDistributed(actual.toArray(Instant[]::new), tick);
        }
    }
}