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

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.component.TypedCommandComponent;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.help.result.CommandEntry;
import org.incendo.cloud.minecraft.extras.ImmutableMinecraftHelp;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;

@DefaultQualifier(NonNull.class)
public final class HelpCommand {
  private final CommandManager<CommandSender> manager;
  private final MinecraftHelp<CommandSender> minecraftHelp;
  private final TypedCommandComponent<CommandSender, String> helpQueryArgument;

  HelpCommand(final CommandManager<CommandSender> mgr) {
    this.manager = mgr;
    this.minecraftHelp = createMinecraftHelp(mgr);
    this.helpQueryArgument = createHelpQueryArgument(mgr);
  }

  public void register() {
    this.manager.command(this.manager.commandBuilder("chess")
      .literal("help")
      .argument(this.helpQueryArgument)
      .permission("chesscraft.command.help")
      .handler(this::executeHelp));
  }

  private void executeHelp(final CommandContext<CommandSender> context) {
    this.minecraftHelp.queryCommands(
      context.optional(this.helpQueryArgument).orElse(""),
      context.sender()
    );
  }

  private static TypedCommandComponent<CommandSender, String> createHelpQueryArgument(final CommandManager<CommandSender> mgr) {
    final var commandHelpHandler = mgr.createHelpHandler();
    final BlockingSuggestionProvider.Strings<CommandSender> suggestions = (context, input) ->
      commandHelpHandler.queryRootIndex(context.sender()).entries().stream()
        .map(CommandEntry::syntax)
        .toList();
    return TypedCommandComponent.<CommandSender, String>builder()
      .name("query")
      .parser(greedyStringParser())
      .suggestionProvider(suggestions)
      .optional()
      .build();
  }

  private static MinecraftHelp<CommandSender> createMinecraftHelp(final CommandManager<CommandSender> mgr) {
    final MinecraftHelp<CommandSender> minecraftHelp = MinecraftHelp.createNative("/chess help", mgr);
    return ImmutableMinecraftHelp.copyOf(minecraftHelp).withColors(MinecraftHelp.helpColors(
      TextColor.color(0x783201),
      NamedTextColor.WHITE,
      TextColor.color(0xB87341),
      NamedTextColor.GRAY,
      NamedTextColor.DARK_GRAY
    ));
  }
}
