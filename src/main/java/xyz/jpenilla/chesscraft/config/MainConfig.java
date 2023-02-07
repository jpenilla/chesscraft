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

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import xyz.jpenilla.chesscraft.util.ProcessorUtil;

@ConfigSerializable
public final class MainConfig {
  private String stockfishEngine = defaultEngine();
  private Messages messages = new Messages();
  private PieceOptions pieces = new PieceOptions.ItemFrame();

  public String stockfishEngine() {
    return this.stockfishEngine;
  }

  public Messages messages() {
    return this.messages;
  }

  public PieceOptions pieces() {
    return this.pieces;
  }

  private static String defaultEngine() {
    return "15.1:" + ProcessorUtil.bestEngine();
  }
}
