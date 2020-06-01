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
import net.kyori.adventure.audience.MultiAudience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.AdventurePlatform;
import net.kyori.adventure.platform.PlatformAudience;
import net.kyori.adventure.platform.ProviderSupport;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

public class BungeePlatform implements AdventurePlatform {

  public static @NonNull PlatformAudience<ProxiedPlayer> player(final @NonNull ProxiedPlayer player) {
    return new PlayerAudience(requireNonNull(player, "player"));
  }

  @Override
  public @NonNull String name() {
    return "BungeeCord";
  }

  @Override
  public @NonNull ProviderSupport supportLevel() {
    return ProviderSupport.LIMITED;
  }

  @Override
  public @NonNull PlatformAudience<CommandSender> console() {
    return new ConsoleAudience(ProxyServer.getInstance().getConsole());
  }

  @Override
  public @NonNull MultiAudience audience(final @NonNull Iterable<Audience> audiences) {
    return MultiAudience.of(audiences);
  }

  @Override
  public @NonNull MultiAudience permission(final @NonNull String permission) {
    return null;
  }

  @Override
  public @NonNull MultiAudience online() {
    return new OnlinePlayersAudience(ProxyServer.getInstance());
  }
}
