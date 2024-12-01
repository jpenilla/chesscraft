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

import java.util.List;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public final class MainConfig {
  private String stockfishEngine = "16:AUTO";
  private DatabaseSettings databaseSettings = new DatabaseSettings();
  private PieceOptions pieces = new PieceOptions.DisplayEntity();
  private List<String> defaultDisplays = List.of("log", "status", "position-labels");
  private Messages messages = new Messages();

  public String stockfishEngine() {
    return this.stockfishEngine;
  }

  public PieceOptions pieces() {
    return this.pieces;
  }

  public List<String> defaultDisplays() {
    return this.defaultDisplays;
  }

  public Messages messages() {
    return this.messages;
  }

  public DatabaseSettings databaseSettings() {
    return this.databaseSettings;
  }
}
