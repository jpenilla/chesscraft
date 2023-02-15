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
package xyz.jpenilla.chesscraft.command.argument;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.standard.DurationArgument;
import cloud.commandframework.context.CommandContext;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.command.Commands;
import xyz.jpenilla.chesscraft.data.TimeControlSettings;
import xyz.jpenilla.chesscraft.util.ComponentRuntimeException;

@DefaultQualifier(NonNull.class)
public final class TimeControlArgument extends CommandArgument<CommandSender, TimeControlSettings> {
  TimeControlArgument(
    final boolean required,
    final String name,
    final String defaultValue,
    final @Nullable BiFunction<CommandContext<CommandSender>, String, List<String>> suggestionsProvider,
    final ArgumentDescription defaultDescription
  ) {
    super(required, name, new Parser(), defaultValue, TimeControlSettings.class, suggestionsProvider, defaultDescription);
  }

  public static Builder builder(final String name) {
    return new Builder(name);
  }

  public static TimeControlArgument create(final String name) {
    return builder(name).asRequired().build();
  }

  public static final class Builder extends CommandArgument.TypedBuilder<CommandSender, TimeControlSettings, Builder> {
    private Builder(final String name) {
      super(TimeControlSettings.class, name);
    }

    @Override
    public TimeControlArgument build() {
      return new TimeControlArgument(
        this.isRequired(),
        this.getName(),
        this.getDefaultValue(),
        this.getSuggestionsProvider(),
        this.getDefaultDescription()
      );
    }
  }

  public static final class Parser implements ArgumentParser<CommandSender, TimeControlSettings> {
    private static final ArgumentParser<CommandSender, Duration> DURATION_PARSER = new DurationArgument.Parser<>();
    private static final List<String> SUGGESTIONS = List.of(
      "10m",
      "5m",
      "3m",
      "1m",
      "15m:10s",
      "10m:5s",
      "3m:2s",
      "2m:1s",
      "1m:1s"
    );

    @Override
    public ArgumentParseResult<TimeControlSettings> parse(
      final CommandContext<CommandSender> commandContext,
      final Queue<String> inputQueue
    ) {
      final String peek = inputQueue.peek();
      final String[] split = peek.split(":");
      if (split.length != 1 && split.length != 2) {
        return ArgumentParseResult.failure(ComponentRuntimeException.withMessage(commandContext.get(Commands.PLUGIN).config().messages().invalidTimeControl(peek)));
      }
      final Queue<String> queue = new LinkedList<>();
      queue.add(split[0]);
      final ArgumentParseResult<Duration> duration0 = DURATION_PARSER.parse(commandContext, queue);
      if (duration0.getFailure().isPresent()) {
        return ArgumentParseResult.failure(duration0.getFailure().get());
      }
      if (split.length == 2) {
        queue.add(split[1]);
        final ArgumentParseResult<Duration> duration1 = DURATION_PARSER.parse(commandContext, queue);
        if (duration1.getFailure().isPresent()) {
          return ArgumentParseResult.failure(duration1.getFailure().get());
        }
        inputQueue.remove();
        return ArgumentParseResult.success(new TimeControlSettings(duration0.getParsedValue().orElseThrow(), duration1.getParsedValue().orElseThrow()));
      }
      inputQueue.remove();
      return ArgumentParseResult.success(new TimeControlSettings(duration0.getParsedValue().orElseThrow(), Duration.ofSeconds(0)));
    }

    @Override
    public List<String> suggestions(
      final CommandContext<CommandSender> commandContext,
      final String input
    ) {
      return SUGGESTIONS;
    }
  }
}
