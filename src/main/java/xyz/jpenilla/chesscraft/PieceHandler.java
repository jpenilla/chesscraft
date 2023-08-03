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
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.jpenilla.chesscraft.config.PieceOptions;
import xyz.jpenilla.chesscraft.data.CardinalDirection;
import xyz.jpenilla.chesscraft.data.Vec3;
import xyz.jpenilla.chesscraft.data.piece.Piece;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;

public interface PieceHandler {
  void applyToWorld(ChessBoard board, BoardStateHolder game, World world);

  void removeFromWorld(ChessBoard board, World world);

  final class DisplayEntity implements PieceHandler {
    private final PieceOptions.DisplayEntity options;

    public DisplayEntity(final PieceOptions.DisplayEntity options) {
      this.options = options;
    }

    @Override
    public void applyToWorld(final ChessBoard board, final BoardStateHolder game, final World world) {
      board.forEachPosition(boardPosition -> {
        final Vec3 pos = board.toWorld(boardPosition);
        removePieceAt(world, board, pos);
        final Piece piece = game.piece(boardPosition);

        if (piece == null) {
          return;
        }
        world.spawn(pos.toLocation(world), ItemDisplay.class, itemDisplay -> {
          itemDisplay.setTransformation(transformationFor(board, piece));
          itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
          itemDisplay.setItemStack(this.options.item(piece));
          itemDisplay.setInvulnerable(true);
          itemDisplay.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, board.name());
        });
        world.spawn(new Location(world, pos.x() + 0.5 * board.scale(), pos.y(), pos.z() + 0.5 * board.scale()), Interaction.class, interaction -> {
          interaction.setInvulnerable(true);
          interaction.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, board.name());
          interaction.setResponsive(true);
          interaction.setInteractionHeight((float) this.options.height(piece.type()) * board.scale());
          interaction.setInteractionWidth(0.5f * board.scale());
        });
      });
    }

    private static Transformation transformationFor(final ChessBoard board, final Piece piece) {
      // flip upwards
      final Quaternionf left = new Quaternionf(new AxisAngle4f((float) Math.toRadians(90.0D), 1, 0, 0));
      transformForVersion(left);
      // rotate
      final Quaternionf right = new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotation(board.facing(), piece)), 0, 0, 1));

      return new Transformation(
        // center
        new Vector3f(0.5f * board.scale(), 0, 0.5f * board.scale()),
        left,
        // scale
        new Vector3f(0.5f * board.scale()),
        right
      );
    }

    private static void transformForVersion(final Quaternionf left) {
      if (Bukkit.getServer().getMinecraftVersion().startsWith("1.19")) {
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
      q.x = z;
      q.y = w;
      q.z = x;
      q.w = -y;
    }

    private static float rotation(final CardinalDirection facing, final Piece piece) {
      final double deg = facing.radians() * 180 / Math.PI;
      if (piece.color() == PieceColor.WHITE) {
        return (float) deg;
      }
      return (float) deg + 180f;
    }

    @Override
    public void removeFromWorld(final ChessBoard board, final World world) {
      board.forEachPosition(pos -> removePieceAt(world, board, board.toWorld(pos)));
    }

    private static void removePieceAt(final World world, final ChessBoard board, final Vec3 pos) {
      final List<Entity> entities = new ArrayList<>();
      entities.addAll(world.getNearbyEntities(
        pos.toLocation(world),
        0.25,
        0.5,
        0.25,
        e -> e instanceof Display
      ));
      entities.addAll(world.getNearbyEntities(
        new Location(world, pos.x() + 0.5 * board.scale(), pos.y(), pos.z() + 0.5 * board.scale()),
        0.25,
        0.5,
        0.25,
        e -> e instanceof Interaction
      ));
      for (final Entity entity : entities) {
        if (entity.getPersistentDataContainer().has(BoardManager.PIECE_KEY)) {
          entity.remove();
        }
      }
    }
  }

  final class ItemFrame implements PieceHandler {
    private final PieceOptions.ItemFrame options;

    public ItemFrame(final PieceOptions.ItemFrame options) {
      this.options = options;
    }

    @Override
    public void applyToWorld(final ChessBoard board, final BoardStateHolder game, final World world) {
      board.forEachPosition(boardPosition -> {
        final Vec3 pos = board.toWorld(boardPosition);
        removePieceAt(world, pos);
        final Piece piece = game.piece(boardPosition);

        if (piece == null) {
          return;
        }
        world.spawn(pos.toLocation(world), org.bukkit.entity.ItemFrame.class, itemFrame -> {
          itemFrame.setRotation(rotation(board.facing(), piece));
          itemFrame.setItem(this.options.item(piece));
          itemFrame.setFacingDirection(BlockFace.UP);
          itemFrame.setInvulnerable(true);
          itemFrame.setVisible(false);
          itemFrame.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, board.name());
        });
        world.spawn(new Location(world, pos.x() + 0.5, pos.y() + this.options.heightOffset(piece.type()), pos.z() + 0.5), ArmorStand.class, stand -> {
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

    private static void removePieceAt(final World world, final Vec3 pos) {
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
