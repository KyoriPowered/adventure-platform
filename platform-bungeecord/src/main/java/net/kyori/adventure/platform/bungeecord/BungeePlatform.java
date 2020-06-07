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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AdventurePlatform;
import net.kyori.adventure.platform.PlatformAudience;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class BungeePlatform implements AdventurePlatform {

  public static @NonNull PlatformAudience<ProxiedPlayer> player(final @NonNull ProxiedPlayer player) {
    return new PlayerAudience(requireNonNull(player, "player"));
  }

  private final ProxyServer proxy;
  private final Audience console;
  private final Audience players;

  public BungeePlatform(final @NonNull ProxyServer proxy) {
    this.proxy = requireNonNull(proxy, "proxy");
    this.console = new ConsoleAudience(proxy.getConsole());
    this.players = new PlayersAudience(proxy);
  }

  @Override
  public @NonNull String name() {
    return proxy.getName() + " " + proxy.getVersion();
  }

  @Override
  public @NonNull Audience console() {
    return console;
  }

  @Override
  public @NonNull Audience players() {
    return players;
  }

  @Override
  public @NonNull Audience player(final @NonNull UUID playerId) {
    final ProxiedPlayer player = proxy.getPlayer(playerId);
    if (player == null) return Audience.empty();
    return new PlayerAudience(player);
  }

  @Override
  public @NonNull Audience permission(final @NonNull String permission) {
    return Audience.empty(); // TODO
  }

  @Override
  public @NonNull Audience server(final @NonNull String serverName) {
    return new PlayersAudience(proxy, requireNonNull(serverName, "server name"));
  }

  @Override
  public @NonNull Audience world(final @NonNull UUID worldId) {
    return Audience.empty(); // Bungee has no concept of worlds, so silently fail
  }
}
