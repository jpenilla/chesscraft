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
package xyz.jpenilla.chesscraft.data;

import java.time.Duration;
import xyz.jpenilla.chesscraft.util.TimeUtil;

public record TimeControlSettings(Duration time, Duration increment) {
  @Override
  public String toString() {
    if (this.increment.toSeconds() < 1) {
      return TimeUtil.formatDurationClock(this.time, TimeUtil.ClockFormatMode.TIME);
    }
    return TimeUtil.formatDurationClock(this.time, TimeUtil.ClockFormatMode.TIME) + "|" + TimeUtil.formatDurationClock(this.increment, TimeUtil.ClockFormatMode.INCREMENT);
  }
}
