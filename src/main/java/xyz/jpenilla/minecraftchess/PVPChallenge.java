package xyz.jpenilla.minecraftchess;

import org.bukkit.entity.Player;

public record PVPChallenge(
  ChessBoard board,
  Player challenger,
  Player player,
  PieceColor challengerColor
) {
  public void accept() {
    final ChessPlayer sender = ChessPlayer.player(this.challenger);
    final ChessPlayer opp = ChessPlayer.player(this.player);
    this.board.startGame(
      this.challengerColor == PieceColor.WHITE ? sender : opp,
      this.challengerColor == PieceColor.BLACK ? sender : opp
    );
  }
}
