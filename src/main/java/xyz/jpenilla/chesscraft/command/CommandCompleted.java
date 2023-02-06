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
package xyz.jpenilla.chesscraft.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.util.ComponentRuntimeException;

@DefaultQualifier(NonNull.class)
public final class CommandCompleted extends ComponentRuntimeException {
  private static final long serialVersionUID = 6491603568537806183L;

  private CommandCompleted(final @Nullable Component message) {
    super(message);
  }

  public static CommandCompleted withoutMessage() {
    return new CommandCompleted(null);
  }

  public static CommandCompleted withMessage(final ComponentLike message) {
    return new CommandCompleted(message.asComponent());
  }
}
