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

import cpufeatures.CpuArchitecture;
import cpufeatures.CpuFeatures;
import java.util.List;
import org.slf4j.Logger;
import xyz.niflheim.stockfish.engine.enums.Variant;

public final class ProcessorUtil {
  private ProcessorUtil() {
  }

  public static Variant bestVariant(final Logger logger) {
    CpuFeatures.load();
    final CpuArchitecture arch = CpuFeatures.getArchitecture();
    final List<String> features = switch (arch) {
      case AARCH64 -> CpuFeatures.getAarch64Info().featureList().stream().map(Object::toString).toList();
      case X86 -> CpuFeatures.getX86Info().featureList().stream().map(Object::toString).toList();
      default -> {
        logger.info("Unable to determine best Stockfish variant for architecture '{}', falling back to default variant.", arch);
        yield List.of();
      }
    };
    if (features.contains("BMI2")) {
      return Variant.BMI2;
    } else if (features.contains("AVX2")) {
      return Variant.AVX2;
    } else if (features.contains("POPCNT")) {
      return Variant.POPCNT;
    }
    return Variant.DEFAULT;
  }
}
