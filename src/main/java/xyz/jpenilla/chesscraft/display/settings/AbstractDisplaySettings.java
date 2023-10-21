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
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.config.TransformationSettings;
import xyz.jpenilla.chesscraft.data.Vec3d;
import xyz.jpenilla.chesscraft.display.BoardDisplay;

@ConfigSerializable
public abstract class AbstractDisplaySettings<S> implements BoardDisplay<S> {
  private boolean removeAfterGame = false;
  private double scale = 2.0D;
  protected TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.CENTER;
  protected Display.Billboard billboard = Display.Billboard.CENTER;
  protected Vec3d offset = Vec3d.ZERO;
  protected boolean visibleThroughWalls = false;
  protected boolean textShadow = false;
  protected boolean applyBackgroundColor = false;
  protected @Nullable Color backgroundColor = Color.FUCHSIA;
  protected TransformationSettings transformationDecomposed = new TransformationSettings();

  @Override
  public boolean removeAfterGame() {
    return this.removeAfterGame;
  }

  protected Vec3d offset(final ChessBoard board) {
    return this.offset.add(0, 0, 1, board.scale());
  }

  public void apply(final TextDisplay display) {
    display.setAlignment(this.alignment);
    display.setBillboard(this.billboard);
    display.setSeeThrough(this.visibleThroughWalls);
    display.setShadowed(this.textShadow);
    if (this.applyBackgroundColor && this.backgroundColor != null) {
      display.setDefaultBackground(false);
      display.setBackgroundColor(this.backgroundColor);
    } else {
      display.setDefaultBackground(true);
      display.setBackgroundColor(null);
    }
    display.setTransformation(this.transformation());
  }

  private Transformation transformation() {
    /*
    new Vector3f(0, 0, 0),
    new Quaternionf(new AxisAngle4f()),
    new Vector3f((float) this.scale),
    new Quaternionf(new AxisAngle4f())
     */
    return new Transformation(
      new Vector3f((float) this.transformationDecomposed.translation.x(), (float) this.transformationDecomposed.translation.y(), (float) this.transformationDecomposed.translation.z()),
      new Quaternionf(this.transformationDecomposed.leftRotation.x(), this.transformationDecomposed.leftRotation.y(), this.transformationDecomposed.leftRotation.z(), this.transformationDecomposed.leftRotation.w()),
      new Vector3f((float) this.scale),
      new Quaternionf(this.transformationDecomposed.rightRotation.x(), this.transformationDecomposed.rightRotation.y(), this.transformationDecomposed.rightRotation.z(), this.transformationDecomposed.rightRotation.w())
    );
  }
}
