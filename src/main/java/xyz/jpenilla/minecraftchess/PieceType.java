package xyz.jpenilla.minecraftchess;

import java.util.Locale;

public enum PieceType {
  PAWN("p"),
  BISHOP("b"),
  KNIGHT("n"),
  ROOK("r"),
  QUEEN("q"),
  KING("k");

  private final String abv;

  PieceType(String abv) {
    this.abv = abv;
  }

  public String lower() {
    return this.abv;
  }

  public String upper() {
    return this.abv.toUpperCase(Locale.ENGLISH);
  }

  public static PieceType type(final String c) {
    for (PieceType value : PieceType.values()) {
      if (value.lower().equalsIgnoreCase(c)) {
        return value;
      }
    }
    throw new IllegalArgumentException();
  }
}
