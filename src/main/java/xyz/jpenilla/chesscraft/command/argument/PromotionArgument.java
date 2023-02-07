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
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.captions.StandardCaptionKeys;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.ParserException;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.data.piece.PieceType;

@DefaultQualifier(NonNull.class)
public final class PromotionArgument extends CommandArgument<CommandSender, PieceType> {
  public static final Set<PieceType> VALID_PROMOTIONS = Set.of(PieceType.BISHOP, PieceType.KNIGHT, PieceType.QUEEN, PieceType.ROOK);

  PromotionArgument(
    final boolean required,
    final String name,
    final String defaultValue,
    final @Nullable BiFunction<CommandContext<CommandSender>, String, List<String>> suggestionsProvider,
    final ArgumentDescription defaultDescription
  ) {
    super(required, name, new Parser(), defaultValue, PieceType.class, suggestionsProvider, defaultDescription);
  }

  public static Builder builder(final String name) {
    return new Builder(name);
  }

  public static PromotionArgument create(final String name) {
    return builder(name).asRequired().build();
  }

  public static final class Builder extends TypedBuilder<CommandSender, PieceType, Builder> {
    private Builder(final String name) {
      super(PieceType.class, name);
    }

    @Override
    public PromotionArgument build() {
      return new PromotionArgument(
        this.isRequired(),
        this.getName(),
        this.getDefaultValue(),
        this.getSuggestionsProvider(),
        this.getDefaultDescription()
      );
    }
  }

  public static final class Parser implements ArgumentParser<CommandSender, PieceType> {
    private static final List<String> SUGGESTIONS = VALID_PROMOTIONS.stream().map(PieceType::name).toList();

    @Override
    public ArgumentParseResult<PieceType> parse(
      final CommandContext<CommandSender> commandContext,
      final Queue<String> inputQueue
    ) {
      final String input = Objects.requireNonNull(inputQueue.peek());

      for (final PieceType value : VALID_PROMOTIONS) {
        if (value.name().equalsIgnoreCase(input)) {
          inputQueue.remove();
          return ArgumentParseResult.success(value);
        }
      }

      return ArgumentParseResult.failure(new ParseException(input, commandContext));
    }

    @Override
    public List<String> suggestions(
      final CommandContext<CommandSender> commandContext,
      final String input
    ) {
      return SUGGESTIONS;
    }

    public static final class ParseException extends ParserException {
      private static final long serialVersionUID = -1175027392043957423L;

      public ParseException(
        final String input,
        final CommandContext<?> context
      ) {
        super(
          EnumArgument.EnumParser.class,
          context,
          StandardCaptionKeys.ARGUMENT_PARSE_FAILURE_ENUM,
          CaptionVariable.of("input", input),
          CaptionVariable.of("acceptableValues", acceptableValues())
        );
      }

      private static String acceptableValues() {
        return VALID_PROMOTIONS.stream()
          .map(e -> e.toString().toLowerCase())
          .collect(Collectors.joining(", "));
      }
    }
  }
}
