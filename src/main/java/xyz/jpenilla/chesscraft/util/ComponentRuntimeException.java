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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@SuppressWarnings("serial")
@DefaultQualifier(NonNull.class)
public class ComponentRuntimeException extends RuntimeException implements ComponentMessageThrowable {
  private final @Nullable Component message;

  protected ComponentRuntimeException(final @Nullable Component message) {
    this.message = message;
  }

  public static ComponentRuntimeException withoutMessage() {
    return new ComponentRuntimeException(null);
  }

  public static ComponentRuntimeException withMessage(final ComponentLike message) {
    return new ComponentRuntimeException(message.asComponent());
  }

  @Override
  public @Nullable Component componentMessage() {
    return this.message;
  }

  @Override
  public String getMessage() {
    return PlainTextComponentSerializer.plainText().serializeOr(this.message, "No message.");
  }
}
