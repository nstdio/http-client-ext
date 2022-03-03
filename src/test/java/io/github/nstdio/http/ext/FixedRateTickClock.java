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
