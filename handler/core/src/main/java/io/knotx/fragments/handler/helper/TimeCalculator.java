package io.knotx.fragments.handler.helper;

import static java.time.Instant.now;

public final class TimeCalculator {

  private TimeCalculator() {
    // Utility class
  }

  public static long executionTime(long startTime) {
    return now().toEpochMilli() - startTime;
  }

}
