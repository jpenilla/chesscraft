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
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerTextures;
import xyz.jpenilla.chesscraft.config.PieceOptions;
import xyz.jpenilla.chesscraft.data.piece.Piece;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;

public interface PieceHandler {
  void applyToWorld(ChessBoard board, ChessGame game, World world);

  void removeFromWorld(ChessBoard board, World world);

  final class ItemFramePieceHandler implements PieceHandler {
    private final PieceOptions.ItemFrame options;

    public ItemFramePieceHandler(final PieceOptions.ItemFrame options) {
      this.options = options;
    }

    @Override
    public void applyToWorld(final ChessBoard board, final ChessGame game, final World world) {
      for (int i = 0; i < 8; i++) {
        for (int x = 0; x < 8; x++) {
          removePieceAt(board, world, i, x);
          final int xx = board.loc().x() + i;
          final int zz = board.loc().z() - x;
          final Piece piece = game.pieces()[i][x];

          if (piece == null) {
            continue;
          }
          world.spawn(new Location(world, xx, board.loc().y(), zz), ItemFrame.class, itemFrame -> {
            final ItemStack stack = new ItemStack(this.options.material());
            stack.editMeta(meta -> {
              final int add = piece.color() == PieceColor.WHITE ? 7 : 1;
              meta.setCustomModelData(piece.type().ordinal() + add);
            });
            itemFrame.setRotation(piece.color() == PieceColor.WHITE ? Rotation.CLOCKWISE : Rotation.COUNTER_CLOCKWISE);
            itemFrame.setItem(stack);
            itemFrame.setFacingDirection(BlockFace.UP);
            itemFrame.setInvulnerable(true);
            itemFrame.setVisible(false);
            itemFrame.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, board.name());
          });
          world.spawn(new Location(world, xx + 0.5, board.loc().y() + this.options.heightOffsets().getOrDefault(piece.type(), 0.0D), zz + 0.5), ArmorStand.class, stand -> {
            stand.setInvulnerable(true);
            stand.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, board.name());
            stand.setGravity(false);
            stand.setInvisible(true);
          });
        }
      }
    }

    @Override
    public void removeFromWorld(final ChessBoard board, final World world) {
      for (int i = 0; i < 8; i++) {
        for (int x = 0; x < 8; x++) {
          removePieceAt(board, world, i, x);
        }
      }
    }

    private static void removePieceAt(final ChessBoard board, final World world, final int i, final int x) {
      final int xx = board.loc().x() + i;
      final int zz = board.loc().z() - x;

      final Collection<Entity> entities = world.getNearbyEntities(
        new Location(world, xx + 0.5, board.loc().y(), zz + 0.5),
        0.25,
        0.5,
        0.25,
        e -> (e instanceof ItemFrame || e instanceof ArmorStand) && e.getPersistentDataContainer().has(BoardManager.PIECE_KEY)
      );
      for (final Entity entity : entities) {
        entity.remove();
      }
    }
  }

  final class PlayerHeadPieceHandler implements PieceHandler {
    private final PieceOptions.PlayerHead options;

    public PlayerHeadPieceHandler(final PieceOptions.PlayerHead options) {
      this.options = options;
    }

    @Override
    public void applyToWorld(final ChessBoard board, final ChessGame game, final World world) {
      for (int i = 0; i < 8; i++) {
        for (int x = 0; x < 8; x++) {
          final int xx = board.loc().x() + i;
          final int zz = board.loc().z() - x;
          final Piece piece = game.pieces()[i][x];

          if (piece == null) {
            world.setType(xx, board.loc().y(), zz, Material.AIR);
            continue;
          }
          world.setType(xx, board.loc().y(), zz, Material.PLAYER_HEAD);
          final Skull state = (Skull) world.getBlockState(xx, board.loc().y(), zz);
          final PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
          final String get = piece.color() == PieceColor.WHITE ? this.options.white().get(piece.type()) : this.options.black().get(piece.type());
          final PlayerTextures textures = profile.getTextures();
          try {
            textures.setSkin(new URL("https://textures.minecraft.net/texture/" + get));
          } catch (final IOException ex) {
            throw new RuntimeException(ex);
          }
          profile.setTextures(textures);
          state.setPlayerProfile(profile);
          state.update();
        }
      }
    }

    @Override
    public void removeFromWorld(final ChessBoard board, final World world) {
      for (int i = 0; i < 8; i++) {
        for (int x = 0; x < 8; x++) {
          final int xx = board.loc().x() + i;
          final int zz = board.loc().z() - x;
          world.setType(xx, board.loc().y(), zz, Material.AIR);
        }
      }
    }
  }
}
