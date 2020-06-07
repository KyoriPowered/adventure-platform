/*
 * This file is part of text-extras, licensed under the MIT License.
 *
 * Copyright (c) 2018 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.kyori.adventure.platform.bungeecord;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.collect.Iterables.transform;
import static java.util.Objects.requireNonNull;

final class PlayersAudience implements MultiAudience {
  private final ProxyServer proxy;
  private final @Nullable String serverName;

  public PlayersAudience(final @NonNull ProxyServer server) {
    this(server, null);
  }

  public PlayersAudience(final @NonNull ProxyServer server, final @Nullable String serverName) {
    this.proxy = requireNonNull(server, "proxy");
    this.serverName = serverName;
  }

  @Override
  public @NonNull Iterable<Audience> audiences() {
    final Collection<ProxiedPlayer> players;
    if (serverName == null) {
      players = this.proxy.getPlayers();
    } else {
      final ServerInfo server = this.proxy.getServerInfo(serverName);
      if (server == null) {
        players = Collections.emptyList();
      } else {
        players = server.getPlayers();
      }
    }
    return transform(players, PlayerAudience::new);
  }
}
