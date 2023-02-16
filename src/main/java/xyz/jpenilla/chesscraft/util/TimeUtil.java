/*
 * chesscraft
 *
 * Copyright (c) 2023 Jason Penilla
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
package xyz.jpenilla.chesscraft.util;

import java.text.DecimalFormat;
import java.time.Duration;

public final class TimeUtil {
  private static final DecimalFormat DF = new DecimalFormat(".00");

  private TimeUtil() {
  }

  public static String formatDurationClock(final Duration d) {
    return formatDurationClock(d, ClockFormatMode.DEFAULT);
  }

  public enum ClockFormatMode {
    DEFAULT, TIME, INCREMENT
  }

  public static String formatDurationClock(final Duration d, final ClockFormatMode mode) {
    final long hours = d.toHours();
    final long minutes = d.toMinutesPart();
    final long seconds = d.toSecondsPart();
    if (hours == 0 && seconds == 0 && mode == ClockFormatMode.TIME) {
      return String.format("%s", minutes);
    } else if (hours == 0 && minutes == 0) {
      switch (mode) {
        case DEFAULT -> {
          if (seconds <= 30) {
            final double partialSeconds = d.toMillisPart() / 1000.0D;
            return String.format("0:%02d%s", seconds, DF.format(partialSeconds));
          }
        }
        case TIME -> {
          return String.format("0:%d", seconds);
        }
        case INCREMENT -> {
          return String.format("%d", seconds);
        }
      }
    } else if (hours > 0) {
      return String.format("%d:%d:%02d", hours, minutes, seconds);
    }
    return String.format("%d:%02d", minutes, seconds);
  }
}
