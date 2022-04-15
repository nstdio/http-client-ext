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
package io.github.nstdio.http.ext

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors.toCollection
import java.util.stream.Collectors.toList
import java.util.stream.IntStream
import kotlin.streams.toList

internal class FixedRateTickClockTest {
  @RepeatedTest(512)
  fun shouldTickAtFixedRate() {
    //given
    val baseInstant = Instant.ofEpochSecond(0)
    val tick = Duration.ofSeconds(5)
    val clock = FixedRateTickClock(baseInstant, ZoneOffset.UTC, tick)

    //when
    val actual = IntStream.range(0, 32)
      .mapToObj { clock.instant() }
      .toList()
      .toTypedArray()

    //then
    assertEvenlyDistributed(actual, tick)
  }

  private fun assertEvenlyDistributed(actual: Array<Instant>, tick: Duration) {
    var i = 0
    val n = actual.size
    while (i < n) {
      for (j in 0 until n) {
        val expectedTick = tick.multipliedBy((j - i).toLong())
        val actualDuration = Duration.between(actual[i], actual[j])
        Assertions.assertThat(actualDuration).isEqualTo(expectedTick)
      }
      i++
    }
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  internal inner class ThreadSafetyTest {
    private val baseInstant = Instant.ofEpochSecond(0)
    private val tick = Duration.ofSeconds(1)
    private val clock = FixedRateTickClock(baseInstant, ZoneOffset.UTC, tick)
    private val nTasks = 64
    private val nThreads = 12
    private val executor = Executors.newFixedThreadPool(nThreads)

    @AfterAll
    fun tearDown() {
      executor.shutdown()
    }

    @RepeatedTest(512)
    fun shouldBeSafe() {
      //when
      val futures = IntStream.range(0, nTasks)
        .mapToObj { executor.submit<Instant> { clock.instant() } }
        .collect(toList())
      val actual = futures.stream()
        .map { f: Future<Instant> ->
          try {
            return@map f[1, TimeUnit.SECONDS]
          } catch (e: Exception) {
            throw RuntimeException(e)
          }
        }
        .sorted()
        .collect(toCollection { LinkedHashSet() })

      //then

      // Implicitly checking clock to not return same instant more than once
      Assertions.assertThat(actual).hasSize(nTasks)
      assertEvenlyDistributed(actual.toTypedArray(), tick)
    }
  }
}