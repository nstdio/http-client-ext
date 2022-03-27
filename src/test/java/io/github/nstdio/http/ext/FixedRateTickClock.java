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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@code Clock} ticking at specified fixed rate. Each invocation of {@link #instant()} or {@link #millis()} will
 * advance this clock.
 */
class FixedRateTickClock extends Clock {
  private final AtomicReference<Instant> currentInstantRef;
  private final ZoneId zone;
  private final Duration tickDuration;

  FixedRateTickClock(Instant baseInstant, ZoneId zone, Duration tickDuration) {
    this.currentInstantRef = new AtomicReference<>(baseInstant);
    this.zone = zone;
    this.tickDuration = tickDuration;
  }

  static FixedRateTickClock of(Clock baseClock, Duration tickDuration) {
    return new FixedRateTickClock(baseClock.instant(), baseClock.getZone(), tickDuration);
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new FixedRateTickClock(currentInstantRef.get(), zone, tickDuration);
  }

  @Override
  public Instant instant() {
    return currentInstantRef.updateAndGet(i -> i.plus(tickDuration));
  }
}
