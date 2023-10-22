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

import java.util.Set;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.joml.Vector3f;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.config.TransformationSettings;
import xyz.jpenilla.chesscraft.data.Vec3d;
import xyz.jpenilla.chesscraft.display.BoardDisplaySettings;

@ConfigSerializable
public abstract class AbstractDisplaySettings<S> implements BoardDisplaySettings<S> {
  private boolean removeAfterGame = false;
  protected double scale = 2.0D;
  protected TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.CENTER;
  protected Display.Billboard billboard = Display.Billboard.CENTER;
  protected Vec3d offset = Vec3d.ZERO;
  private Set<Axis> scaleOffsetWithBoard = Set.of(Axis.X, Axis.Z);
  private boolean visibleThroughWalls = false;
  protected boolean textShadow = false;
  protected boolean applyBackgroundColor = false;
  protected @Nullable Color backgroundColor = Color.FUCHSIA;

  @Override
  public boolean removeAfterGame() {
    return this.removeAfterGame;
  }

  // nudge offset to make it from the exterior corner of the board
  public Vec3d offsetNudged(final ChessBoard board) {
    return this.offset(board, board.loc().asVec3d()).add(0, 0, 1, board.scale());
  }

  public Vec3d offset(final ChessBoard board, final Vec3d pos) {
    final int xScale = this.scaleOffsetWithBoard.contains(Axis.X) ? board.scale() : 1;
    final int yScale = this.scaleOffsetWithBoard.contains(Axis.Y) ? board.scale() : 1;
    final int zScale = this.scaleOffsetWithBoard.contains(Axis.Z) ? board.scale() : 1;
    return pos.add(this.offset.x() * xScale, this.offset.y() * yScale, this.offset.z() * zScale);
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
  }

  public Transformation transformation(final TransformationSettings settings, final boolean flip) {
    /*
    new Vector3f(0, 0, 0),
    new Quaternionf(new AxisAngle4f()),
    new Vector3f((float) this.scale),
    new Quaternionf(new AxisAngle4f())
     */
    return new Transformation(
      new Vector3f((float) settings.translation().x(), (float) settings.translation().y(), (float) settings.translation().z()),
      settings.leftRotation().asQuaternionf(),
      flip ? new Vector3f((float) this.scale * -1f, (float) this.scale, (float) this.scale * -1) : new Vector3f((float) this.scale),
      settings.rightRotation().asQuaternionf()
    );
  }

  // not all sub subclasses want to expose a transformation to config
  @ConfigSerializable
  public static abstract class WithTransformation<S> extends AbstractDisplaySettings<S> {
    private TransformationSettings transformationDecomposed = new TransformationSettings();

    @Override
    public void apply(final TextDisplay display) {
      super.apply(display);
      display.setTransformation(this.transformation(this.transformationDecomposed, false));
    }
  }

  public enum Axis {
    X, Y, Z
  }
}
