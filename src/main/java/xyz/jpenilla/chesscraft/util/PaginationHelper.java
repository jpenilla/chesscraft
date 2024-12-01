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

import java.util.function.IntFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.chesscraft.ChessCraft;
import xyz.jpenilla.chesscraft.config.Messages;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

@DefaultQualifier(NonNull.class)
public final class PaginationHelper {
  private final ChessCraft plugin;

  public PaginationHelper(final ChessCraft plugin) {
    this.plugin = plugin;
  }

  private Messages messages() {
    return this.plugin.config().messages();
  }

  public Pagination.Builder.Renderer footerRenderer(final IntFunction<String> commandFunction) {
    return (currentPage, pages) -> {
      if (pages == 1) {
        return empty(); // we don't need to see 'Page 1/1'
      }
      final TextComponent.Builder buttons = text();
      if (currentPage > 1) {
        buttons.append(this.previousPageButton(currentPage, commandFunction));
      }
      if (currentPage > 1 && currentPage < pages) {
        buttons.append(space());
      }
      if (currentPage < pages) {
        buttons.append(this.nextPageButton(currentPage, commandFunction));
      }
      return this.messages().paginationFooter(currentPage, pages, buttons);
    };
  }

  public Pagination.Builder.Renderer pageOutOfRange() {
    return (page, pages) -> this.messages().pageOutOfRange(page, pages);
  }

  public Component wrapElement(final ComponentLike element) {
    return textOfChildren(text(" - ", GRAY), element);
  }

  private Component previousPageButton(final int currentPage, final IntFunction<String> commandFunction) {
    return text()
      .content("←")
      .clickEvent(runCommand(commandFunction.apply(currentPage - 1)))
      .hoverEvent(this.messages().clickForPreviousPage())
      .build();
  }

  private Component nextPageButton(final int currentPage, final IntFunction<String> commandFunction) {
    return text()
      .content("→")
      .clickEvent(runCommand(commandFunction.apply(currentPage + 1)))
      .hoverEvent(this.messages().clickForNextPage())
      .build();
  }
}
