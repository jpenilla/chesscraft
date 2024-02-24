package xyz.jpenilla.chesscraft.db.type;

import xyz.jpenilla.chesscraft.data.Fen;

public final class FenColumnMapper extends GsonColumnMapper<Fen> {
  public FenColumnMapper() {
    super(Fen.class);
  }
}
