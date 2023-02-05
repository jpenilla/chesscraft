package xyz.jpenilla.minecraftchess.data.piece;

import java.util.Locale;

public enum PieceType {
  PAWN("p"),
  BISHOP("b"),
  KNIGHT("n"),
  ROOK("r"),
  QUEEN("q"),
  KING("k");

  private final String abv;
  private final String abvUpper;

  PieceType(final String abv) {
    this.abv = abv;
    this.abvUpper = abv.toUpperCase(Locale.ENGLISH);
  }

  public String lower() {
    return this.abv;
  }

  public String upper() {
    return this.abvUpper;
  }
}
