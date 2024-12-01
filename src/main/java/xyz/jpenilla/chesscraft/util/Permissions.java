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

public final class Permissions {
  private Permissions() {
  }

  public static final String PREFIX = "chesscraft.";
  public static final String COMMAND_PREFIX = PREFIX + "command.";

  public static final String COMMAND_VERSION = COMMAND_PREFIX + "version";
  public static final String COMMAND_BOARDS = COMMAND_PREFIX + "boards";
  public static final String COMMAND_RELOAD = COMMAND_PREFIX + "reload";
  public static final String COMMAND_CREATE_BOARD = COMMAND_PREFIX + "create_board";
  public static final String COMMAND_SET_CHECKERBOARD = COMMAND_PREFIX + "set_checkerboard";
  public static final String COMMAND_DELETE_BOARD = COMMAND_PREFIX + "delete_board";
  public static final String COMMAND_CHALLENGE_CPU = COMMAND_PREFIX + "challenge.cpu";
  public static final String COMMAND_CHALLENGE_PLAYER = COMMAND_PREFIX + "challenge.player";
  public static final String COMMAND_ACCEPT = COMMAND_PREFIX + "accept";
  public static final String COMMAND_DENY = COMMAND_PREFIX + "deny";
  public static final String COMMAND_NEXT_PROMOTION = COMMAND_PREFIX + "next_promotion";
  public static final String COMMAND_SHOW_LEGAL_MOVES = COMMAND_PREFIX + "show_legal_moves";
  public static final String COMMAND_FORFEIT = COMMAND_PREFIX + "forfeit";
  public static final String COMMAND_RESET_BOARD = COMMAND_PREFIX + "reset_board";
  public static final String COMMAND_CPU_MATCH = COMMAND_PREFIX + "cpu_match";
  public static final String COMMAND_CANCEL_MATCH = COMMAND_PREFIX + "cancel_match";
  public static final String COMMAND_PAUSE_MATCH = COMMAND_PREFIX + "pause_match";
  public static final String COMMAND_ACCEPT_PAUSE = COMMAND_PREFIX + "accept_pause";
  public static final String COMMAND_RESUME_MATCH = COMMAND_PREFIX + "resume_match";
  public static final String COMMAND_PAUSED_MATCHES_SELF = COMMAND_PREFIX + "paused_matches.self";
  public static final String COMMAND_PAUSED_MATCHES_OTHERS = COMMAND_PREFIX + "paused_matches.others";
  public static final String COMMAND_MATCH_HISTORY_SELF = COMMAND_PREFIX + "match_history.self";
  public static final String COMMAND_MATCH_HISTORY_OTHERS = COMMAND_PREFIX + "match_history.others";
  public static final String COMMAND_EXPORT_MATCH = COMMAND_PREFIX + "export_match";
  public static final String COMMAND_EXPORT_MATCH_OTHERS = COMMAND_PREFIX + "export_match.others";
  public static final String COMMAND_EXPORT_MATCH_INCOMPLETE = COMMAND_PREFIX + "export_match.incomplete";
  public static final String COMMAND_LEADERBOARD = COMMAND_PREFIX + "leaderboard";
}
