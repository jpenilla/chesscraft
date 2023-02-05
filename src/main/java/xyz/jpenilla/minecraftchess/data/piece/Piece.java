package xyz.jpenilla.minecraftchess.data.piece;

public record Piece(PieceType type, PieceColor color) {
  public static Piece decode(final String s) {
    for (final PieceType type : PieceType.values()) {
      if (type.upper().equals(s)) {
        return new Piece(type, PieceColor.WHITE);
      } else if (type.lower().equals(s)) {
        return new Piece(type, PieceColor.BLACK);
      }
    }
    throw new IllegalArgumentException(s);
  }
}
