package xyz.jpenilla.chesscraft.db.type;

import xyz.jpenilla.chesscraft.data.TimeControlSettings;

public final class TimeControlSettingsColumnMapper extends GsonColumnMapper<TimeControlSettings> {
  public TimeControlSettingsColumnMapper() {
    super(TimeControlSettings.class);
  }
}
