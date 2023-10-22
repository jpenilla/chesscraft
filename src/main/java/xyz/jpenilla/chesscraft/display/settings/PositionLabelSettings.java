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
package xyz.jpenilla.chesscraft.display.settings;

import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.config.TransformationSettings;
import xyz.jpenilla.chesscraft.data.Rotation;
import xyz.jpenilla.chesscraft.data.Vec3d;
import xyz.jpenilla.chesscraft.display.PositionLabelsDisplay;

@ConfigSerializable
public final class PositionLabelSettings extends AbstractDisplaySettings<PositionLabelsDisplay> {
  private static final double SHIFT = 5.0 / 32.0;

  private final transient TransformationSettings eastWestTransform;
  private final transient TransformationSettings northSouthTransform;

  public String labelFormat = "<white><label>";

  // todo labels are slightly off-center to the right
  public PositionLabelSettings() {
    this.offset = new Vec3d(0, 0.01, 0);
    this.billboard = Display.Billboard.FIXED;
    this.textShadow = true;
    this.applyBackgroundColor = true;
    this.backgroundColor = Color.fromARGB(0, 0, 0, 0);

    this.eastWestTransform = new TransformationSettings(
      Vec3d.ZERO,
      new Rotation(0, -0.707, 0, 0.707), // rotate
      new Rotation(-0.707, 0, 0, 0.707) // face upwards
    );
    this.northSouthTransform = new TransformationSettings(
      Vec3d.ZERO,
      Rotation.DEFAULT,
      new Rotation(-0.707, 0, 0, 0.707) // face upwards
    );
  }

  @Override
  public DisplayType type() {
    return DisplayType.POSITION_LABELS;
  }

  @Override
  public PositionLabelsDisplay getOrCreateState(final ChessCraft plugin, final ChessBoard board) {
    return new PositionLabelsDisplay(plugin, board, this);
  }

  @Override
  public void gameEnded(final PositionLabelsDisplay state) {
    if (this.removeAfterGame()) {
      state.remove();
    }
  }

  public Transformation eastWestTransformation(final boolean flip, final int boardScale) {
    final TransformationSettings s = this.eastWestTransform.withTranslation(
      new Vec3d((flip ? SHIFT : -SHIFT) * this.scale + 0.5f * boardScale, 0, 0.5f * boardScale)
    );
    return this.transformation(s, flip);
  }

  public Transformation northSouthTransformation(final boolean flip, final int boardScale) {
    final TransformationSettings s = this.northSouthTransform.withTranslation(
      new Vec3d(0.5f * boardScale, 0, (flip ? -SHIFT : SHIFT) * this.scale + 0.5f * boardScale)
    );
    return this.transformation(s, flip);
  }
}
