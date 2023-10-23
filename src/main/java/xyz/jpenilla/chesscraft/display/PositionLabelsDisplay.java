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
package xyz.jpenilla.chesscraft.display;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.data.BoardPosition;
import xyz.jpenilla.chesscraft.data.CardinalDirection;
import xyz.jpenilla.chesscraft.display.settings.PositionLabelSettings;

@DefaultQualifier(NonNull.class)
public final class PositionLabelsDisplay extends AbstractTextDisplayHolder {
  private static final String[] FILE_LABELS = new String[]{"A", "B", "C", "D", "E", "F", "G", "H"};
  private static final String[] RANK_LABELS = new String[]{"8", "7", "6", "5", "4", "3", "2", "1"};

  private final JavaPlugin plugin;
  private final ChessBoard board;
  private final PositionLabelSettings settings;
  private final List<Label> displays;

  public PositionLabelsDisplay(
    final JavaPlugin plugin,
    final ChessBoard board,
    final PositionLabelSettings settings
  ) {
    super(plugin, new Location(board.world(), 0, 0, 0));
    this.plugin = plugin;
    this.board = board;
    this.settings = settings;
    this.displays = this.makeDisplays();
  }

  private List<Label> makeDisplays() {
    final List<Label> list = new ArrayList<>();
    this.board.forEachPosition(pos -> {
      @Nullable BoardPosition rLabelPos = null;
      @Nullable BoardPosition cLabelPos = null;
      if (pos.file() == 0) {
        rLabelPos = new BoardPosition(pos.rank(), -1);
      } else if (pos.file() == 7) {
        rLabelPos = new BoardPosition(pos.rank(), 8);
      }
      if (pos.rank() == 0) {
        cLabelPos = new BoardPosition(-1, pos.file());
      } else if (pos.rank() == 7) {
        cLabelPos = new BoardPosition(8, pos.file());
      }
      final boolean southWest = this.board.facing() == CardinalDirection.SOUTH || this.board.facing() == CardinalDirection.WEST;
      if (rLabelPos != null) {
        list.add(this.label(rLabelPos, RANK_LABELS[pos.rank()], southWest ? pos.file() == 0 : pos.file() == 7, this.board));
      }
      if (cLabelPos != null) {
        list.add(this.label(cLabelPos, FILE_LABELS[pos.file()], southWest ? pos.rank() == 7 : pos.rank() == 0, this.board));
      }
    });
    return list;
  }

  private Label label(
    final BoardPosition bPos,
    final String label,
    final boolean flip,
    final ChessBoard board
  ) {
    return new Label(
      this.plugin,
      this.settings.offset(board, board.toWorld(bPos.vec())).toLocation(this.board.world()),
      MiniMessage.miniMessage().deserialize(this.settings.labelFormat, Placeholder.unparsed("label", label)),
      flip,
      board
    );
  }

  public ChessBoard board() {
    return this.board;
  }

  @Override
  public void remove() {
    for (final Label display : this.displays) {
      display.remove();
    }
  }

  @Override
  public void ensureSpawned() {
    for (final Label display : this.displays) {
      display.ensureSpawned();
    }
  }

  @Override
  public void stopUpdates() {
  }

  @Override
  public void updateNow() {
  }

  @Override
  protected void updateEntity(final TextDisplay display) {
    throw new UnsupportedOperationException();
  }

  private final class Label extends AbstractTextDisplayHolder {
    private final Component label;
    private final Transformation transformation;

    Label(
      final JavaPlugin plugin,
      final Location pos,
      final Component label,
      final boolean flip,
      final ChessBoard board
    ) {
      super(plugin, pos);
      this.label = label;
      this.transformation = this.makeTransformation(board, flip);
    }

    @Override
    public void stopUpdates() {
    }

    @Override
    public void updateNow() {
    }

    @Override
    protected void updateEntity(final TextDisplay display) {
      putMarker(display);
      PositionLabelsDisplay.this.settings.apply(display);

      display.setTransformation(this.transformation);
      display.text(this.label);
    }

    private Transformation makeTransformation(final ChessBoard board, final boolean flip) {
      final PositionLabelSettings settings = PositionLabelsDisplay.this.settings;
      if (board.facing() == CardinalDirection.EAST || board.facing() == CardinalDirection.WEST) {
        return settings.eastWestTransformation(flip, board.scale());
      }
      return settings.northSouthTransformation(flip, board.scale());
    }
  }
}
