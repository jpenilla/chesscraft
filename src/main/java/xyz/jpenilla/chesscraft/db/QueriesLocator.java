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
package xyz.jpenilla.chesscraft.db;

import com.google.common.base.Splitter;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jdbi.v3.core.locator.ClasspathSqlLocator;
import org.jdbi.v3.core.locator.internal.ClasspathBuilder;

@DefaultQualifier(NonNull.class)
public final class QueriesLocator {
  private static final String PREFIX = "queries/";
  private static final Splitter SPLITTER = Splitter.on(';');
  private final ClasspathSqlLocator locator = ClasspathSqlLocator.create();

  public List<String> queries(final String name) {
    return SPLITTER.splitToList(this.query(name));
  }

  public String query(final String name) {
    return this.locate(PREFIX + name);
  }

  private String locate(final String name) {
    return this.locator.getResource(
      QueriesLocator.class.getClassLoader(),
      new ClasspathBuilder()
        .appendDotPath(name)
        .setExtension("sql")
        .build());
  }
}
