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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import xyz.jpenilla.chesscraft.data.piece.Piece;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;
import xyz.jpenilla.chesscraft.data.piece.PieceType;

interface PieceHandler {
  void applyToWorld(ChessBoard board, ChessGame game, World world);

  void removeFromWorld(ChessBoard board, World world);

  final class ItemFramePieceHandler implements PieceHandler {
    private static final Map<PieceType, Double> OFFSETS = new HashMap<>();

    static {
      OFFSETS.put(PieceType.PAWN, -10.0D / 16.0D);
      OFFSETS.put(PieceType.BISHOP, -3.0D / 16.0D);
      OFFSETS.put(PieceType.KNIGHT, -7.0D / 16.0D);
      OFFSETS.put(PieceType.ROOK, -10.0D / 16.0D);
      OFFSETS.put(PieceType.QUEEN, -1.0D / 16.0D);
      OFFSETS.put(PieceType.KING, 0.0D);
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
            final ItemStack stack = new ItemStack(Material.PAPER);
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
          world.spawn(new Location(world, xx + 0.5, board.loc().y() + OFFSETS.get(piece.type()), zz + 0.5), ArmorStand.class, stand -> {
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

  /*
  private static final class ArmorStandPieceHandler implements PieceHandler {
    @Override
    public void applyToWorld(final ChessGame game, final World world) {
      PieceHandler.applyCheckerboard(game, world);
      for (int i = 0; i < 8; i++) {
        for (int x = 0; x < 8; x++) {
          final int xx = game.board.loc().x() + i;
          final int zz = game.board.loc().z() - x;
          final Piece piece = game.pieces[i][x];

          final Collection<Entity> stand = world.getNearbyEntities(new Location(world, xx + 0.5, game.board.loc().y(), zz + 0.5), 0.25, 0.25, 0.25, e -> e instanceof ArmorStand);
          for (final Entity entity : stand) {
            entity.remove();
          }

          if (piece == null) {
            continue;
          }

          world.spawn(new Location(world, xx + 0.5, game.board.loc().y(), zz + 0.5), ArmorStand.class, armorStand -> {
            armorStand.setCustomNameVisible(true);
            armorStand.customName(Component.text(piece.type().name()));
            if (piece.color() == PieceColor.WHITE) {
              armorStand.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            } else {
              armorStand.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            }
            armorStand.setRotation(90, 0);
            armorStand.setInvulnerable(true);
            armorStand.getPersistentDataContainer().set(BoardManager.PIECE_KEY, PersistentDataType.STRING, game.board.name());
            armorStand.setGravity(false);
          });
        }
      }
    }
  }

  private static final class SignPieceHandler implements PieceHandler {
    @Override
    public void applyToWorld(final ChessGame game, final World world) {
      PieceHandler.applyCheckerboard(game, world);
      for (int i = 0; i < 8; i++) {
        for (int x = 0; x < 8; x++) {
          final int xx = game.board.loc().x() + i;
          final int zz = game.board.loc().z() - x;
          final Piece piece = game.pieces[i][x];

          if (piece == null) {
            world.setType(xx, game.board.loc().y(), zz, Material.AIR);
          } else {
            world.setType(xx, game.board.loc().y(), zz, piece.color() == PieceColor.WHITE ? Material.BIRCH_SIGN : Material.DARK_OAK_SIGN);
            final Sign blockState = (Sign) world.getBlockState(xx, game.board.loc().y(), zz);
            blockState.line(0, Component.text(piece.type().name()));
            if (piece.color() == PieceColor.BLACK) {
              blockState.setColor(DyeColor.WHITE);
            }
            blockState.update();
          }
        }
      }
    }
  }
  */
}
