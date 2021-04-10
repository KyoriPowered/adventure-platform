/*
 * This file is part of adventure-platform, licensed under the MIT License.
 *
 * Copyright (c) 2018-2020 KyoriPowered
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

import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.AudienceIdentity;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

final class BungeeIdentity implements AudienceIdentity {
  private final static CommandSender CONSOLE = ProxyServer.getInstance().getConsole();
  private final CommandSender sender;
  private final ProxiedPlayer player;

  BungeeIdentity(final @NotNull CommandSender sender) {
    this.sender = requireNonNull(sender, "command sender");
    this.player = this.player() ? (ProxiedPlayer) sender : null;
  }

  @Override
  public @NotNull UUID uuid() {
    if(this.player()) {
      return this.player.getUniqueId();
    }
    return Identity.nil().uuid();
  }

  @Override
  public @NotNull Locale locale() {
    if(this.player()) {
      return this.player.getLocale();
    }
    return Locale.US;
  }

  @Override
  public @Nullable String world() {
    return null; // Bungee has no concept of worlds
  }

  @Override
  public @Nullable String server() {
    if(this.player()) {
      final Server server = this.player.getServer();
      if(server != null) {
        final ServerInfo info = server.getInfo();
        if(info != null) {
          return info.getName();
        }
      }
    }
    return null;
  }

  @Override
  public boolean player() {
    return this.sender instanceof ProxiedPlayer;
  }

  @Override
  public boolean console() {
    return this.sender.equals(CONSOLE);
  }

  @Override
  public boolean permission(final @NotNull String key) {
    return this.sender.hasPermission(key);
  }

  @Override
  public int hashCode() {
    return this.sender.hashCode();
  }
}
