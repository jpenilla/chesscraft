package xyz.jpenilla.minecraftchess.data;

import org.bukkit.entity.Player;
import xyz.jpenilla.minecraftchess.ChessBoard;
import xyz.jpenilla.minecraftchess.data.piece.PieceColor;

public record PVPChallenge(
  ChessBoard board,
  Player challenger,
  Player player,
  PieceColor challengerColor
) {}
