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
package xyz.jpenilla.chesscraft;

import com.destroystokyo.paper.profile.PlayerProfile;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.util.Transformation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.jpenilla.chesscraft.config.PieceOptions;
import xyz.jpenilla.chesscraft.data.BoardPosition;
import xyz.jpenilla.chesscraft.data.CardinalDirection;
import xyz.jpenilla.chesscraft.data.Vec3i;
import xyz.jpenilla.chesscraft.data.piece.Piece;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.data.piece.PieceType;
import xyz.jpenilla.chesscraft.util.MatchExporter;
import xyz.jpenilla.chesscraft.util.Reflection;
import xyz.jpenilla.chesscraft.util.SteppedAnimation;

public interface PieceHandler {
  void applyToWorld(ChessBoard board, BoardStateHolder game, World world);

  default void applyMoveToWorld(
    final ChessBoard board,
    final BoardStateHolder game,
    final World world,
    final ChessGame.Move move
  ) {
    this.applyToWorld(board, game, world);
  }

  void removeFromWorld(ChessBoard board, World world);

  final class DisplayEntity implements PieceHandler {
    private static final int SHRINK_DURATION = 12;
    private static final int TELEPORT_DURATION = 5;

    private final ChessCraft plugin;
    private final PieceOptions.DisplayEntity options;

    public DisplayEntity(final ChessCraft plugin, final PieceOptions.DisplayEntity options) {
      this.plugin = plugin;
      this.options = options;
    }

    @Override
    public void applyToWorld(final ChessBoard board, final BoardStateHolder game, final World world) {
      board.forEachPosition(boardPosition -> {
        final Piece piece = game.piece(boardPosition);
        final Vec3i pos = board.toWorld(boardPosition);
        if (piece == null) {
          removePieceAt(world, board, pos);
          return;
        }

        final List<Entity> existing = pieceAt(world, board, pos);
        final Consumer<ItemDisplay> displayConfigure = itemDisplay -> {
          this.configureItemDisplay(board, itemDisplay, piece);
          itemDisplay.setTeleportDuration(0);
        };
        final @Nullable ItemDisplay existingItemDisplay = (ItemDisplay) existing.stream().filter(it -> it instanceof ItemDisplay).findFirst().orElse(null);
        if (existingItemDisplay != null) {
          displayConfigure.accept(existingItemDisplay);
          existingItemDisplay.teleport(pos.toLocation(world));
        } else {
          Reflection.spawn(pos.toLocation(world), ItemDisplay.class, displayConfigure);
        }

        final Consumer<Interaction> interactionConfigure = interaction -> this.configureInteraction(board, interaction, piece);
        final @Nullable Interaction existingInteraction = (Interaction) existing.stream().filter(it -> it instanceof Interaction).findFirst().orElse(null);
        final Location interactionLoc = interactionLoc(board, world, pos);
        if (existingInteraction != null) {
          interactionConfigure.accept(existingInteraction);
          existingInteraction.teleport(interactionLoc);
        } else {
          Reflection.spawn(interactionLoc, Interaction.class, interactionConfigure);
        }
      });
    }

    private void configureInteraction(final ChessBoard board, final Interaction interaction, final Piece piece) {
      interaction.setInvulnerable(true);
      interaction.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, board.name());
      interaction.setResponsive(true);
      interaction.setInteractionHeight((float) this.options.height(piece.type()) * board.scale());
      interaction.setInteractionWidth(0.5f * board.scale());
    }

    private void configureItemDisplay(final ChessBoard board, final ItemDisplay itemDisplay, final Piece piece) {
      itemDisplay.setTransformation(transformationFor(board, piece));
      itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
      itemDisplay.setItemStack(this.options.item(piece));
      itemDisplay.setInvulnerable(true);
      itemDisplay.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, board.name());
    }

    @Override
    public void applyMoveToWorld(final ChessBoard board, final BoardStateHolder game, final World world, final ChessGame.Move move) {
      board.animationExecutor().schedule(() -> this.makeAnimation(board, game, world, move));
    }

    private @Nullable SteppedAnimation makeAnimation(final ChessBoard board, final BoardStateHolder game, final World world, final ChessGame.Move move) {
      final BoardPosition fromPos = BoardPosition.fromString(move.notation().substring(0, 2));
      final BoardPosition toPos = BoardPosition.fromString(move.notation().substring(2, 4));

      // Collect captured pieces (or rook in case of castle)
      final List<List<Entity>> captures = new ArrayList<>();
      board.forEachPosition(pos -> {
        if (!pos.equals(fromPos) && game.piece(pos) == null) {
          final List<Entity> entities = pieceAt(world, board, board.toWorld(pos));
          if (entities.size() == 2) {
            captures.add(entities);
          }
        }
      });
      final List<Entity> destEntities = pieceAt(world, board, board.toWorld(toPos));
      if (destEntities.size() == 2) {
        captures.add(destEntities);
      }

      final List<Entity> movedPieceEntities = pieceAt(world, board, board.toWorld(fromPos));

      if (movedPieceEntities.size() != 2) {
        this.inconsistentState(board, game, world, move);
        return null;
      }

      final @Nullable Piece movedPiece = game.piece(toPos);
      if (movedPiece == null) {
        this.inconsistentState(board, game, world, move);
        return null;
      }

      final SteppedAnimation.Builder animation = new SteppedAnimation.Builder();

      this.movePiece(board, world, movedPieceEntities, toPos, movedPiece, animation);

      // castling
      if (movedPiece.type() == PieceType.KING && Math.abs(toPos.file() - fromPos.file()) == 2) {
        if (captures.size() != 1 || captures.get(0).size() != 2) {
          this.inconsistentState(board, game, world, move);
          return null;
        }
        final List<Entity> rook = captures.get(0);
        final BoardPosition rookDest = new BoardPosition(toPos.rank(), toPos.file() == 2 ? 3 : 5);
        this.movePiece(board, world, rook, rookDest, game.piece(rookDest), animation);
      } else {
        for (final List<Entity> capture : captures) {
          for (final Entity entity : capture) {
            if (entity instanceof ItemDisplay display) {
              display.setInterpolationDuration(SHRINK_DURATION);
              display.setInterpolationDelay(-1);
              // ensure interpolation duration change is sent
              animation.step(0, () -> {
                final Transformation transformation = display.getTransformation();
                final Vector3f scale = transformation.getScale().mul(0.01f);
                display.setTransformation(new Transformation(
                  transformation.getTranslation(),
                  transformation.getLeftRotation(),
                  scale,
                  transformation.getRightRotation()
                ));
              }).then(SHRINK_DURATION, display::remove);
            } else {
              entity.remove();
            }
          }
        }
      }

      return animation
        // ensure interpolation duration change is sent
        .startDelay(1)
        .build();
    }

    private void inconsistentState(
      final ChessBoard board,
      final BoardStateHolder game,
      final World world,
      final ChessGame.Move move
    ) {
      this.plugin.getSLF4JLogger().warn(
        "Found inconsistent board state (move: {}), reapplying entire board\n{}",
        move.notation(),
        MatchExporter.writePgn(board.game().snapshotState(null), this.plugin.database()).join(),
        new Throwable()
      );
      this.applyToWorld(board, game, world);
    }

    private void movePiece(
      final ChessBoard board,
      final World world,
      final List<Entity> movedPiece,
      final BoardPosition toPos,
      final Piece destPiece,
      final SteppedAnimation.Builder animation
    ) {
      for (final Entity entity : movedPiece) {
        if (entity instanceof ItemDisplay display) {
          display.setTeleportDuration(TELEPORT_DURATION);
          // ensure teleport duration change is sent
          animation.step(0, () -> {
            display.teleport(board.toWorld(toPos).toLocation(world));
            world.playSound(board.moveSound(), display.getX(), display.getY(), display.getZ());
            display.setTeleportDuration(0);
          });
          // needed in case of promotion
          this.configureItemDisplay(board, display, destPiece);
        } else if (entity instanceof Interaction interaction) {
          final Vec3i pos = board.toWorld(toPos);
          entity.teleport(interactionLoc(board, world, pos));
          // needed in case of promotion
          this.configureInteraction(board, interaction, destPiece);
        }
      }
    }

    private static Location interactionLoc(final ChessBoard board, final World world, final Vec3i pos) {
      return new Location(world, pos.x() + 0.5 * board.scale(), pos.y(), pos.z() + 0.5 * board.scale());
    }

    private static Transformation transformationFor(final ChessBoard board, final Piece piece) {
      // flip upwards
      final Quaternionf left = new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1, 0, 0));
      transformForVersion(left);
      // rotate
      final Quaternionf right = new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotation(board, piece)), 0, 0, 1));

      return new Transformation(
        // center
        new Vector3f(0.5f * board.scale(), 0, 0.5f * board.scale()),
        left,
        // scale
        new Vector3f(0.5f * board.scale()),
        right
      );
    }

    private static final boolean v1_19_X = Bukkit.getServer().getMinecraftVersion().startsWith("1.19");

    private static void transformForVersion(final Quaternionf left) {
      if (v1_19_X) {
        // 1.19.x - nothing to do
        return;
      }
      // 1.20+ - account for 180 flip on y-axis
      rotateYFlip(left);
    }

    /**
     * Transforms the given quaternion in-place to account for the
     * changes Mojang made to item displays in 1.20.
     *
     * @param q quaternion
     */
    private static void rotateYFlip(final Quaternionf q) {
      final float x = q.x, y = q.y, z = q.z, w = q.w;
      q.x = -z;
      q.y = w;
      q.z = x;
      q.w = -y;
    }

    private static float rotation(final ChessBoard board, final Piece piece) {
      final boolean eastWest = !v1_19_X && (board.facing() == CardinalDirection.EAST || board.facing() == CardinalDirection.WEST);

      if (piece.color() == PieceColor.WHITE) {
        return (float) board.facing().degrees() + (eastWest ? 180 : 0);
      }
      return (float) board.facing().degrees() + (eastWest ? 0 : 180);
    }

    @Override
    public void removeFromWorld(final ChessBoard board, final World world) {
      board.forEachPosition(pos -> removePieceAt(world, board, board.toWorld(pos)));
    }

    private static void removePieceAt(final World world, final ChessBoard board, final Vec3i pos) {
      for (final Entity entity : pieceAt(world, board, pos)) {
        if (entity.getPersistentDataContainer().has(BoardManager.PIECE_KEY)) {
          entity.remove();
        }
      }
    }
  }

  private static List<Entity> pieceAt(final World world, final ChessBoard board, final Vec3i pos) {
    final List<Entity> entities = new ArrayList<>();
    entities.addAll(world.getNearbyEntities(
      pos.toLocation(world),
      0.25,
      0.5,
      0.25,
      e -> e instanceof Display
    ));
    entities.addAll(world.getNearbyEntities(
      DisplayEntity.interactionLoc(board, world, pos),
      0.25,
      0.5,
      0.25,
      e -> e instanceof Interaction
    ));
    entities.removeIf(entity -> !entity.getPersistentDataContainer().has(BoardManager.PIECE_KEY));
    return entities;
  }

  final class ItemFrame implements PieceHandler {
    private final PieceOptions.ItemFrame options;

    public ItemFrame(final PieceOptions.ItemFrame options) {
      this.options = options;
    }

    @Override
    public void applyToWorld(final ChessBoard board, final BoardStateHolder game, final World world) {
      board.forEachPosition(boardPosition -> {
        final Vec3i pos = board.toWorld(boardPosition);
        removePieceAt(world, pos);
        final Piece piece = game.piece(boardPosition);

        if (piece == null) {
          return;
        }
        Reflection.spawn(pos.toLocation(world), org.bukkit.entity.ItemFrame.class, itemFrame -> {
          itemFrame.setRotation(rotation(board.facing(), piece));
          itemFrame.setItem(this.options.item(piece));
          itemFrame.setFacingDirection(BlockFace.UP);
          itemFrame.setInvulnerable(true);
          itemFrame.setVisible(false);
          itemFrame.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, board.name());
        });
        Reflection.spawn(new Location(world, pos.x() + 0.5, pos.y() + this.options.heightOffset(piece.type()), pos.z() + 0.5), ArmorStand.class, stand -> {
          stand.setInvulnerable(true);
          stand.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, board.name());
          stand.setGravity(false);
          stand.setInvisible(true);
        });
      });
    }

    private static Rotation rotation(final CardinalDirection facing, final Piece piece) {
      Rotation rot = Rotation.NONE;
      // translate
      final double deg = facing.radians() * 180 / Math.PI;
      final int rots = (int) (deg / 45);
      for (int i = 0; i < rots; i++) {
        rot = rot.rotateClockwise();
      }
      if (piece.color() == PieceColor.BLACK) {
        return rot;
      }
      // flip
      for (int i = 0; i < 4; i++) {
        rot = rot.rotateClockwise();
      }
      return rot;
    }

    @Override
    public void removeFromWorld(final ChessBoard board, final World world) {
      board.forEachPosition(pos -> removePieceAt(world, board.toWorld(pos)));
    }

    private static void removePieceAt(final World world, final Vec3i pos) {
      final Collection<Entity> entities = world.getNearbyEntities(
        new Location(world, pos.x() + 0.5, pos.y(), pos.z() + 0.5),
        0.25,
        0.5,
        0.25,
        e -> (e instanceof org.bukkit.entity.ItemFrame || e instanceof ArmorStand) && e.getPersistentDataContainer().has(BoardManager.PIECE_KEY)
      );
      for (final Entity entity : entities) {
        entity.remove();
      }
    }
  }

  final class PlayerHead implements PieceHandler {
    private final PieceOptions.PlayerHead options;

    public PlayerHead(final PieceOptions.PlayerHead options) {
      this.options = options;
    }

    @Override
    public void applyToWorld(final ChessBoard board, final BoardStateHolder game, final World world) {
      board.forEachPosition(boardPosition -> {
        final Location loc = board.toWorld(boardPosition).toLocation(world);
        final Piece piece = game.piece(boardPosition);

        if (piece == null) {
          world.setType(loc, Material.AIR);
          return;
        }
        world.setType(loc, Material.PLAYER_HEAD);
        final Skull state = (Skull) world.getBlockState(loc);
        final PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        final PlayerTextures textures = profile.getTextures();
        try {
          textures.setSkin(new URL("https://textures.minecraft.net/texture/" + this.options.texture(piece)));
        } catch (final IOException ex) {
          throw new RuntimeException(ex);
        }
        profile.setTextures(textures);
        state.setPlayerProfile(profile);
        state.update();
      });
    }

    @Override
    public void removeFromWorld(final ChessBoard board, final World world) {
      board.forEachPosition(boardPosition -> world.setType(board.toWorld(boardPosition).toLocation(world), Material.AIR));
    }
  }
}
