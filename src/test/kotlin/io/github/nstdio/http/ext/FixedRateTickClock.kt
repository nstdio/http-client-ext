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

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

/**
 * The `Clock` ticking at specified fixed rate. Each invocation of [.instant] or [.millis] will
 * advance this clock.
 */
internal class FixedRateTickClock(baseInstant: Instant, zone: ZoneId, tickDuration: Duration) : Clock() {
  private val currentInstantRef: AtomicReference<Instant>
  private val zone: ZoneId
  private val tickDuration: Duration

  init {
    currentInstantRef = AtomicReference(baseInstant)
    this.zone = zone
    this.tickDuration = tickDuration
  }

  override fun getZone(): ZoneId {
    return zone
  }

  override fun withZone(zone: ZoneId): Clock {
    return FixedRateTickClock(currentInstantRef.get(), zone, tickDuration)
  }

  override fun instant(): Instant {
    return currentInstantRef.updateAndGet { i: Instant -> i.plus(tickDuration) }
  }

  companion object {
    @JvmStatic
    fun of(baseClock: Clock, tickDuration: Duration): FixedRateTickClock {
      return FixedRateTickClock(baseClock.instant(), baseClock.zone, tickDuration)
    }
  }
}