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
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import xyz.niflheim.stockfish.engine.enums.Variant;

@ConfigSerializable
public record MainConfig(String stockfishEngine) {
  public MainConfig() {
    this("15.1:" + bestEngine());
  }

  private static Variant bestEngine() {
    final CentralProcessor.ProcessorIdentifier procId = new SystemInfo().getHardware().getProcessor().getProcessorIdentifier();
    if (procId.getVendor().contains("AMD")) {
      final int family = Integer.parseInt(procId.getFamily());
      if (family == 23) {
        // Zen 1-2
        return Variant.AVX2;
      } else if (family >= 25) {
        // Zen 3+
        return Variant.BMI2;
      }
    } else if (procId.getVendor().contains("Intel")) {
      if (procId.getFamily().equals("6") && Integer.parseInt(procId.getModel()) >= 60) {
        // Haswell+
        return Variant.AVX2;
        // return Variant.BMI2; // supposed to be right - but get stream is closed errors, need to look closer into this
      }
    }
    return Variant.DEFAULT;
  }
}
