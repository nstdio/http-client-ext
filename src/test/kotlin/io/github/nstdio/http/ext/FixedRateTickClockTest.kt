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

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Executors

internal class FixedRateTickClockTest {
  @RepeatedTest(16)
  fun shouldTickAtFixedRate() {
    //given
    val baseInstant = Instant.ofEpochSecond(0)
    val tick = Duration.ofSeconds(5)
    val clock = FixedRateTickClock(baseInstant, ZoneOffset.UTC, tick)

    //when
    val actual = (0..32)
      .map { clock.instant() }
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
        actualDuration shouldBe expectedTick
      }
      i++
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
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

    @RepeatedTest(16)
    fun shouldBeSafe() {
      //when
      val actual = (0 until nTasks)
        .map { executor.submit<Instant> { clock.instant() } }
        .map { it.get() }
        .toSortedSet()

      //then

      // Implicitly checking clock to not return same instant more than once
      actual shouldHaveSize nTasks
      assertEvenlyDistributed(actual.toTypedArray(), tick)
    }
  }
}