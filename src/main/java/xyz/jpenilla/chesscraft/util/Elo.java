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
package xyz.jpenilla.chesscraft.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public final class Elo {
  public static final int INITIAL_RATING = 1000;

  public enum MatchOutcome {
    PLAYER_ONE_WIN,
    PLAYER_TWO_WIN,
    DRAW
  }

  public record NewRatings(RatingData playerOne, RatingData playerTwo) {}

  public static NewRatings computeNewRatings(
    final Elo.@Nullable RatingData playerOne,
    final Elo.@Nullable RatingData playerTwo,
    final MatchOutcome result
  ) {
    final Match match = new Match(
      playerOne == null ? RatingData.newPlayer() : playerOne,
      playerTwo == null ? RatingData.newPlayer() : playerTwo,
      result
    );
    return match.computeNewRatings();
  }

  private record Match(
    RatingData playerOne,
    RatingData playerTwo,
    MatchOutcome result
  ) {
    public NewRatings computeNewRatings() {
      return new NewRatings(
        this.playerOne.update(this.score(this.playerOne), this.playerTwo.rating()),
        this.playerTwo.update(this.score(this.playerTwo), this.playerOne.rating())
      );
    }

    private double score(final RatingData player) {
      if (this.result == MatchOutcome.DRAW) {
        return 0.5;
      } else if ((this.result == MatchOutcome.PLAYER_ONE_WIN && player == this.playerOne)
        || (this.result == MatchOutcome.PLAYER_TWO_WIN && player == this.playerTwo)) {
        return 1.0;
      }
      return 0.0;
    }
  }

  public record RatingData(int rating, int peakRating, int matches) {
    private RatingData update(final double score, final int opponentRating) {
      final double ratingDelta = this.k() * (score - this.winOdds(opponentRating));
      final int newRating = this.rating + (int) Math.round(ratingDelta);
      return new RatingData(
        newRating,
        Math.max(newRating, this.peakRating),
        this.matches + 1
      );
    }

    public double winOdds(final int opponentRating) {
      final int ratingDiff = Math.max(Math.min(opponentRating - this.rating(), 400), -400);
      return 1.0 / (1 + Math.pow(10, ratingDiff / 400.0));
    }

    private double k() {
      if (this.matches < 30 && this.peakRating < 2400) {
        return 40;
      } else if (this.peakRating < 2400) {
        return 20;
      }
      return 10;
    }

    public static RatingData newPlayer() {
      return new RatingData(INITIAL_RATING, INITIAL_RATING, 0);
    }
  }
}
