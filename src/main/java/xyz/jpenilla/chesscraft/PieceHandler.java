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
import java.util.Collection;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerTextures;
import xyz.jpenilla.chesscraft.config.PieceOptions;
import xyz.jpenilla.chesscraft.data.Vec3;
import xyz.jpenilla.chesscraft.data.piece.Piece;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;

public interface PieceHandler {
  void applyToWorld(ChessBoard board, ChessGame game, World world);

  void removeFromWorld(ChessBoard board, World world);

  final class ItemFrame implements PieceHandler {
    private final PieceOptions.ItemFrame options;

    public ItemFrame(final PieceOptions.ItemFrame options) {
      this.options = options;
    }

    @Override
    public void applyToWorld(final ChessBoard board, final ChessGame game, final World world) {
      board.forEachPosition(boardPosition -> {
        final Vec3 pos = board.loc(boardPosition);
        removePieceAt(world, pos);
        final Piece piece = game.piece(boardPosition);

        if (piece == null) {
          return;
        }
        world.spawn(pos.toLocation(world), org.bukkit.entity.ItemFrame.class, itemFrame -> {
          itemFrame.setRotation(piece.color() == PieceColor.WHITE ? Rotation.CLOCKWISE : Rotation.COUNTER_CLOCKWISE);
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

    @Override
    public void removeFromWorld(final ChessBoard board, final World world) {
      board.forEachPosition(pos -> removePieceAt(world, board.loc(pos)));
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
    public void applyToWorld(final ChessBoard board, final ChessGame game, final World world) {
      board.forEachPosition(boardPosition -> {
        final Location loc = board.loc(boardPosition).toLocation(world);
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
      board.forEachPosition(boardPosition -> world.setType(board.loc(boardPosition).toLocation(world), Material.AIR));
    }
  }
}
