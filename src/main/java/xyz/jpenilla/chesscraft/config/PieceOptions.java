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
package xyz.jpenilla.chesscraft.config;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import xyz.jpenilla.chesscraft.PieceHandler;
import xyz.jpenilla.chesscraft.data.piece.PieceType;

public interface PieceOptions {
  Serializer SERIALIZER = new Serializer();

  PieceHandler createHandler();

  Mode mode();

  enum Mode {
    ITEM_FRAME(ItemFrame.class),
    PLAYER_HEAD(PlayerHead.class);

    private final Class<? extends PieceOptions> type;

    Mode(final Class<? extends PieceOptions> type) {
      this.type = type;
    }
  }

  @ConfigSerializable
  final class ItemFrame implements PieceOptions {
    private Material material = Material.PAPER;
    private Map<PieceType, Double> heightOffsets = Map.of(
      PieceType.PAWN, -10.0D / 16.0D,
      PieceType.BISHOP, -3.0D / 16.0D,
      PieceType.KNIGHT, -7.0D / 16.0D,
      PieceType.ROOK, -10.0D / 16.0D,
      PieceType.QUEEN, -1.0D / 16.0D,
      PieceType.KING, 0.0D
    );

    public Material material() {
      return this.material;
    }

    public Map<PieceType, Double> heightOffsets() {
      return this.heightOffsets;
    }

    @Override
    public PieceHandler createHandler() {
      return new PieceHandler.ItemFramePieceHandler(this);
    }

    @Override
    public Mode mode() {
      return Mode.ITEM_FRAME;
    }
  }

  @ConfigSerializable
  final class PlayerHead implements PieceOptions {
    private Map<PieceType, String> white = Map.of(
      PieceType.PAWN, "4b7aa4969ae3df8334958c212050be343854303767c29d1a1d11c4ac7b7b53d4",
      PieceType.BISHOP, "4ddacb1907d3ef3f304210b4f9ab75bd4fc1c9f21b25350904d00cd949f19388",
      PieceType.KNIGHT, "5a550c4f2626f47a9270bd82ee8ce70189ff2941900ac8934dcfe88f2d2181d2",
      PieceType.ROOK, "7ca26c1c0e19b736ba22f7b31157af920fed61830bd20471b4a886ec971c6323",
      PieceType.QUEEN, "c33b3ce86d571690b19f5d6d7aa0a721eec70a77962730c771a0815d71503899",
      PieceType.KING, "209b3d23a7af275a56402123428186e52965ca9373336ddc112792b415aba0a0"
    );
    private Map<PieceType, String> black = Map.of(
      PieceType.PAWN, "c108c58582bcf2166cd2c0858c73b0af5d1b081f9d628c1b076972c6eb6f0e30",
      PieceType.BISHOP, "cf9dc78ddc4d732f5e3ed634f74b3ed0b811ed6d52f38c13f2f6db3e99ac084f",
      PieceType.KNIGHT, "8c5decd64c2324945e9214eed2297eddb626c436543fbd413930f12874f2b801",
      PieceType.ROOK, "2817755e0a13d5460d183130a5fcdd2e5c38e7292bcd6ef5fdc990501812ac39",
      PieceType.QUEEN, "a62cfaf38deaee3d08d965ce6dc580c8ccd75d3b14d1bfeb4e093726b6c7b1e2",
      PieceType.KING, "7ec2822c66ea3a523a8b3c6820580c8c44fdf373b5dc3f55f70028d6cf6d2e44"
    );

    public Map<PieceType, String> white() {
      return this.white;
    }

    public Map<PieceType, String> black() {
      return this.black;
    }

    @Override
    public PieceHandler createHandler() {
      return new PieceHandler.PlayerHeadPieceHandler(this);
    }

    @Override
    public Mode mode() {
      return Mode.PLAYER_HEAD;
    }
  }

  final class Serializer implements TypeSerializer<PieceOptions> {
    private static final String MODE_FIELD_NAME = "mode";

    private Serializer() {
    }

    @Override
    public PieceOptions deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
      final Mode mode = node.node(MODE_FIELD_NAME).get(Mode.class);
      if (mode == null) {
        throw new SerializationException("Missing mode, should be one of: " + Arrays.toString(Mode.values()));
      }
      return node.get(mode.type);
    }

    @Override
    public void serialize(final Type type, final @Nullable PieceOptions obj, final ConfigurationNode node) throws SerializationException {
      if (obj == null) {
        throw new SerializationException("null");
      }
      final Mode mode = obj.mode();
      node.node(MODE_FIELD_NAME).set(mode);
      node.set(mode.type, obj);
    }
  }
}
