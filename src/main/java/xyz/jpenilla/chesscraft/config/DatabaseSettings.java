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
package xyz.jpenilla.chesscraft.config;

import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@DefaultQualifier(NonNull.class)
@ConfigSerializable
public class DatabaseSettings {
  public DatabaseSettings() {
  }

  public DatabaseSettings(final String url, final String username, final String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  @Comment("Database type. When using H2, none of the other settings in this section apply.")
  public DatabaseType type = DatabaseType.H2;

  @Comment("""
    JDBC URL. Suggested defaults for each DB:
    MariaDB: jdbc:mariadb://host:3306/<database_name>""")
  public String url = "jdbc:mariadb://localhost:3306/chesscraft";

  @Comment("The connection username.")
  public String username = "username";

  @Comment("The connection password.")
  public String password = "password";

  @Comment("Settings for the connection pool. This is an advanced configuration that most users won't need to touch.")
  public ConnectionPool connectionPool = new ConnectionPool();

  public enum DatabaseType {
    H2, MARIADB
  }

  @ConfigSerializable
  public static class ConnectionPool {
    public int maximumPoolSize = 8;
    public int minimumIdle = 8;
    public long maximumLifetime = TimeUnit.MINUTES.toMillis(30);
    public long keepaliveTime = 0L;
    public long connectionTimeout = TimeUnit.SECONDS.toMillis(30);
  }
}
