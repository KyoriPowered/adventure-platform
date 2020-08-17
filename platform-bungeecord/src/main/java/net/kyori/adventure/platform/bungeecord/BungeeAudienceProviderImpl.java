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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.facet.FacetAudienceProvider;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.SettingsChangedEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class BungeeAudienceProviderImpl extends FacetAudienceProvider<CommandSender, BungeeAudience> implements BungeeAudienceProvider {
  private final Plugin plugin;
  private final Listener listener;

  BungeeAudienceProviderImpl(final Plugin plugin) {
    this.plugin = requireNonNull(plugin, "plugin");
    this.listener = new Listener();
    this.plugin.getProxy().getPluginManager().registerListener(this.plugin, this.listener);

    final CommandSender console = this.plugin.getProxy().getConsole();
    this.addViewer(console);
    this.changeViewer(console, Locale.getDefault());

    for(final ProxiedPlayer player : this.plugin.getProxy().getPlayers()) {
      this.addViewer(player);
    }
  }

  @NonNull
  @Override
  public Audience sender(final @NonNull CommandSender sender) {
    if(sender instanceof ProxiedPlayer) {
      return this.player((ProxiedPlayer) sender);
    } else if(this.isConsole(sender)) {
      return this.console();
    }
    return this.createAudience(Collections.singletonList(sender));
  }

  @NonNull
  @Override
  public Audience player(final @NonNull ProxiedPlayer player) {
    return this.player(player.getUniqueId());
  }

  @Override
  protected @Nullable UUID hasId(final @NonNull CommandSender viewer) {
    if(viewer instanceof ProxiedPlayer) {
      return ((ProxiedPlayer) viewer).getUniqueId();
    }
    return null;
  }

  @Override
  protected boolean isConsole(final @NonNull CommandSender viewer) {
    return ProxyServer.getInstance().getConsole().equals(viewer);
  }

  @Override
  protected boolean hasPermission(final @NonNull CommandSender viewer, final @NonNull String permission) {
    return viewer.hasPermission(permission);
  }

  @Override
  protected boolean isInWorld(final @NonNull CommandSender viewer, final @NonNull Key world) {
    return false;
  }

  @Override
  protected boolean isOnServer(final @NonNull CommandSender viewer, final @NonNull String server) {
    if(viewer instanceof ProxiedPlayer) {
      return ((ProxiedPlayer) viewer).getServer().getInfo().getName().equals(server);
    }
    return false;
  }

  @NonNull
  @Override
  protected BungeeAudience createAudience(final @NonNull Collection<CommandSender> viewers) {
    return new BungeeAudience(viewers);
  }

  @Override
  public void close() {
    this.plugin.getProxy().getPluginManager().unregisterListener(this.listener);
    super.close();
  }

  public final class Listener implements net.md_5.bungee.api.plugin.Listener {
    @EventHandler(priority = Byte.MIN_VALUE /* before EventPriority.LOWEST */)
    public void onLogin(final PostLoginEvent event) {
      BungeeAudienceProviderImpl.this.addViewer(event.getPlayer());
    }

    @EventHandler(priority = Byte.MAX_VALUE /* after EventPriority.HIGHEST */)
    public void onDisconnect(final PlayerDisconnectEvent event) {
      BungeeAudienceProviderImpl.this.removeViewer(event.getPlayer());
    }

    @EventHandler(priority = Byte.MAX_VALUE /* after EventPriority.HIGHEST */)
    public void onSettingsChanged(final SettingsChangedEvent event) {
      BungeeAudienceProviderImpl.this.changeViewer(event.getPlayer(), event.getPlayer().getLocale());
    }
  }
}
