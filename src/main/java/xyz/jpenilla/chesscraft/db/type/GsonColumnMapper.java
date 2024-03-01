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
package xyz.jpenilla.chesscraft.db.type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import xyz.jpenilla.chesscraft.data.Fen;
import xyz.jpenilla.chesscraft.util.Util;

public abstract class GsonColumnMapper<T>
  extends AbstractArgumentFactory<T>
  implements ColumnMapper<T> {

  private final Gson gson = GsonComponentSerializer.gson().populator().apply(new GsonBuilder())
    .registerTypeAdapter(Fen.class, new Fen.JsonSerializer())
    .registerTypeAdapter(Duration.class, new DurationAdapter())
    .create();
  private final Type type;

  public GsonColumnMapper(final Type type) {
    super(Types.VARCHAR);
    this.type = type;
  }

  @Override
  protected Argument build(final T value, final ConfigRegistry config) {
    return (position, statement, ctx) -> statement.setString(position, this.gson.toJson(value));
  }

  @Override
  public T map(final ResultSet r, final int columnNumber, final StatementContext ctx) throws SQLException {
    final @Nullable String string = Util.trim(r.getString(columnNumber));

    if (string != null) {
      return this.gson.fromJson(string, this.type);
    }

    return null;
  }

  private static final class DurationAdapter implements JsonSerializer<Duration>, JsonDeserializer<Duration> {
    @Override
    public Duration deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
      return Duration.ofMillis(json.getAsLong());
    }

    @Override
    public JsonElement serialize(final Duration src, final Type typeOfSrc, final JsonSerializationContext context) {
      return context.serialize(src.toMillis());
    }
  }
}
