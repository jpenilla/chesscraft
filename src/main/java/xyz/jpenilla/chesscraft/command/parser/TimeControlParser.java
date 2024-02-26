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
package xyz.jpenilla.chesscraft.command.parser;

import java.time.Duration;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.standard.DurationParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import xyz.jpenilla.chesscraft.command.Commands;
import xyz.jpenilla.chesscraft.data.TimeControlSettings;
import xyz.jpenilla.chesscraft.util.ComponentRuntimeException;

@DefaultQualifier(NonNull.class)
public final class TimeControlParser implements ArgumentParser<CommandSender, TimeControlSettings>, BlockingSuggestionProvider.Strings<CommandSender> {
  private static final ArgumentParser<CommandSender, Duration> DURATION_PARSER = DurationParser.<CommandSender>durationParser().parser();
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

  public static ParserDescriptor<CommandSender, TimeControlSettings> timeControlParser() {
    return ParserDescriptor.of(new TimeControlParser(), TimeControlSettings.class);
  }

  @Override
  public ArgumentParseResult<TimeControlSettings> parse(
    final CommandContext<CommandSender> commandContext,
    final CommandInput commandInput
  ) {
    final String peek = commandInput.readString();
    final String[] split = peek.split(":");
    if (split.length != 1 && split.length != 2) {
      return ArgumentParseResult.failure(ComponentRuntimeException.withMessage(commandContext.get(Commands.PLUGIN).config().messages().invalidTimeControl(peek)));
    }
    final ArgumentParseResult<Duration> duration0 = DURATION_PARSER.parse(commandContext, CommandInput.of(split[0]));
    if (duration0.failure().isPresent()) {
      return ArgumentParseResult.failure(duration0.failure().get());
    }
    if (split.length == 2) {
      final ArgumentParseResult<Duration> duration1 = DURATION_PARSER.parse(commandContext, CommandInput.of(split[1]));
      if (duration1.failure().isPresent()) {
        return ArgumentParseResult.failure(duration1.failure().get());
      }
      return ArgumentParseResult.success(new TimeControlSettings(duration0.parsedValue().orElseThrow(), duration1.parsedValue().orElseThrow()));
    }
    return ArgumentParseResult.success(new TimeControlSettings(duration0.parsedValue().orElseThrow(), Duration.ofSeconds(0)));
  }

  @Override
  public List<String> stringSuggestions(
    final CommandContext<CommandSender> commandContext,
    final CommandInput commandInput
  ) {
    return SUGGESTIONS;
  }
}
