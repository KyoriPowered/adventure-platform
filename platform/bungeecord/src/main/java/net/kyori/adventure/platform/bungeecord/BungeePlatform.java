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

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AdventurePlatformImpl;
import net.kyori.adventure.platform.impl.JDKLogHandler;
import net.kyori.adventure.platform.impl.Knobs;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

public class BungeePlatform extends AdventurePlatformImpl implements Listener {

  static final int PROTCOOL_1_9 = 107;
  static final int PROTOCOL_1_16 = 735;

  private static final Map<String, BungeePlatform> INSTANCES = new ConcurrentHashMap<>();
  
  static {
    Knobs.logger(new JDKLogHandler());
  }

  public static BungeePlatform of(final @NonNull Plugin plugin) {
    requireNonNull(plugin, "A plugin instance is required");

    final String key = plugin.getDescription().getName().toLowerCase(Locale.ROOT);
    BungeePlatform platform = INSTANCES.get(key);
    if(platform == null) {
      platform = new BungeePlatform(key, plugin);
      final BungeePlatform existing = INSTANCES.putIfAbsent(key, platform);
      if(existing != null) {
        return existing;
      }
      platform.init();
    }
    return platform;
  }

  private final String key;
  private final Plugin plugin;
  private final BungeeBossBarListener bossBars = new BungeeBossBarListener();

  BungeePlatform(final String key, final Plugin plugin) {
    this.key = requireNonNull(key, "key");
    this.plugin = requireNonNull(plugin, "plugin");
  }

  private void init() {
    try {
      this.plugin.getProxy().getPluginManager().registerListener(this.plugin, this);
    } catch(Exception ex) {
      Knobs.logError("registering events with plugin", ex);
    }
    add(new BungeeSenderAudience(this.plugin.getProxy().getConsole()));
  }

  /* package */ ProxyServer proxy() {
    return this.plugin.getProxy();
  }

  /* package */ BungeeBossBarListener bossBars() {
    return this.bossBars;
  }

  @EventHandler(priority = Byte.MIN_VALUE /* before EventPriority.LOWEST */)
  public void onLogin(PostLoginEvent event) {
    this.add(new BungeePlayerAudience(this, event.getPlayer()));
  }

  @EventHandler(priority = Byte.MAX_VALUE /* after EventPriority.HIGHEST */)
  public void onQuit(PlayerDisconnectEvent event) {
    this.remove(event.getPlayer().getUniqueId());
    this.bossBars.hideAll(event.getPlayer());
  }

  public Audience audience(final @NonNull CommandSender sender)  {
    requireNonNull(sender, "sender");
    if(sender instanceof ProxiedPlayer) {
      return player(((ProxiedPlayer) sender).getUniqueId());
    } else if(sender == this.plugin.getProxy().getConsole()) {
      return console();
    } else {
      return new BungeeSenderAudience(sender);
    }
  }

  @Override
  public void close() {
    INSTANCES.remove(this.key);
    this.plugin.getProxy().getPluginManager().unregisterListener(this);
    this.bossBars.hideAll();
    super.close();
  }
}
