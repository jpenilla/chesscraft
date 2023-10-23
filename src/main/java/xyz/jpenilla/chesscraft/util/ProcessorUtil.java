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

import java.util.Locale;
import org.bytedeco.cpu_features.X86Features;
import org.bytedeco.cpu_features.global.cpu_features;
import org.slf4j.Logger;
import xyz.niflheim.stockfish.engine.enums.Variant;

public final class ProcessorUtil {
  private ProcessorUtil() {
  }

  public static Variant bestVariant(final Logger logger, final boolean bmi2Available) {
    final CpuArchitecture arch = CpuArchitecture.get();
    switch (arch) {
      case AARCH64 -> {
        // POPCNT builds are called 'modern' with >=SF16, and we want macOS modern build as well
        return Variant.POPCNT;
      }
      case X86 -> {
        final X86Features features = cpu_features.GetX86Info().features();
        if (bmi2Available && features.bmi2() != 0) {
          return Variant.BMI2;
        } else if (features.avx2() != 0) {
          return Variant.AVX2;
        } else if (features.popcnt() != 0) {
          return Variant.POPCNT;
        }
      }
      default ->
        logger.info("Unable to determine best Stockfish variant for architecture '{}', falling back to default variant.", arch);
    }
    return Variant.DEFAULT;
  }

  private enum CpuArchitecture {
    UNKNOWN, AARCH64, ARM, X86;

    public static CpuArchitecture get() {
      final String arch = System.getProperty("os.arch")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "");
      return switch (arch) {
        case "aarch64" -> AARCH64;
        case "arm", "arm32" -> ARM;
        case "x8664", "amd64", "ia32e", "em64t", "x64" -> X86;
        default -> UNKNOWN;
      };
    }
  }
}
