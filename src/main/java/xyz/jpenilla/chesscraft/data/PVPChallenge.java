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
package xyz.jpenilla.chesscraft.data;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.chesscraft.ChessBoard;
import xyz.jpenilla.chesscraft.ChessPlayer;
import xyz.jpenilla.chesscraft.GameState;
import xyz.jpenilla.chesscraft.data.piece.PieceColor;

public interface PVPChallenge {
  ChessBoard board();

  Player challenger();

  Player player();

  PieceColor challengerColor();

  default ChessPlayer white() {
    return ChessPlayer.player(this.challengerColor() == PieceColor.WHITE ? this.challenger() : this.player());
  }

  default ChessPlayer black() {
    return ChessPlayer.player(this.challengerColor() == PieceColor.BLACK ? this.challenger() : this.player());
  }

  @Nullable TimeControlSettings timeControl();

  interface ResumeMatch extends PVPChallenge {
    GameState state();
  }
}
