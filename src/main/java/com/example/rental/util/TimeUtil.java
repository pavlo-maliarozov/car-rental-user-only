package com.example.rental.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class TimeUtil {
    private TimeUtil() {}
    public static Instant endFromStartAndDays(Instant startAt, int days) {
        return startAt.plus(days, ChronoUnit.DAYS);
    }
}
