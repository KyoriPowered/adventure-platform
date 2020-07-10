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

import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AbstractAdventurePlatform;
import net.kyori.adventure.platform.impl.JDKLogHandler;
import net.kyori.adventure.platform.impl.Knobs;
import net.kyori.adventure.text.serializer.bungeecord.BungeeCordComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
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

/* package */ final class BungeePlatform extends AbstractAdventurePlatform implements BungeeAudiences, Listener {

  /* package */ static BungeePlatform of(final @NonNull Plugin plugin) {
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

  static final int PROTCOOL_1_9 = 107;
  static final int PROTOCOL_1_16 = 735;

  private static final Map<String, BungeePlatform> INSTANCES = new ConcurrentHashMap<>();

  static {
    // Inject our adapter component into Bungee's Gson instance
    // (this is separate from the ComponentSerializer gson instance~)
    // please just use Velocity
    try {
      final Field gsonField = ProxyServer.getInstance().getClass().getDeclaredField("gson");
      gsonField.setAccessible(true);
      final Gson gson = (Gson) gsonField.get(ProxyServer.getInstance());
      BungeeCordComponentSerializer.inject(gson);
    } catch(final NoSuchFieldException | IllegalAccessException | ClassCastException e) {
      Knobs.logError("Injecting into BungeeCord (ProxyServer) gson instance", e);
    }

    Knobs.logger(new JDKLogHandler());
  }

  private final String key;
  private final Plugin plugin;
  private final BungeeBossBarListener bossBars = new BungeeBossBarListener();
  private final Listener listener;

  BungeePlatform(final String key, final Plugin plugin) {
    this.key = requireNonNull(key, "key");
    this.plugin = requireNonNull(plugin, "plugin");
    this.listener = new Listener();
  }

  private void init() {
    try {
      this.plugin.getProxy().getPluginManager().registerListener(this.plugin, this.listener);
    } catch(final Exception ex) {
      Knobs.logError("registering events with plugin", ex);
    }
    this.add(new BungeeSenderAudience(this.plugin.getProxy().getConsole()));
    for(final ProxiedPlayer player : this.plugin.getProxy().getPlayers()) {
      this.add(new BungeePlayerAudience(this, player));
    }
  }

  /* package */ ProxyServer proxy() {
    return this.plugin.getProxy();
  }

  /* package */ BungeeBossBarListener bossBars() {
    return this.bossBars;
  }

  public final class Listener implements net.md_5.bungee.api.plugin.Listener {
    /* package */ Listener() {
    }

    @EventHandler(priority = Byte.MIN_VALUE /* before EventPriority.LOWEST */)
    public void onLogin(final PostLoginEvent event) {
      BungeePlatform.this.add(new BungeePlayerAudience(BungeePlatform.this, event.getPlayer()));
    }

    @EventHandler(priority = Byte.MAX_VALUE /* after EventPriority.HIGHEST */)
    public void onQuit(final PlayerDisconnectEvent event) {
      BungeePlatform.this.remove(event.getPlayer().getUniqueId());
      BungeePlatform.this.bossBars.hideAll(event.getPlayer());
    }

  }

  @Override
  public @NonNull Audience player(final @NonNull ProxiedPlayer player) {
    return this.player(requireNonNull(player, "player").getUniqueId());
  }

  @Override
  public @NonNull Audience audience(final @NonNull CommandSender sender) {
    requireNonNull(sender, "sender");
    if(sender instanceof ProxiedPlayer) {
      return this.player(((ProxiedPlayer) sender).getUniqueId());
    } else if(sender == this.plugin.getProxy().getConsole()) {
      return this.console();
    } else {
      return new BungeeSenderAudience(sender);
    }
  }

  @Override
  public @NonNull GsonComponentSerializer gsonSerializer() {
    return GsonComponentSerializer.gson(); // TODO: maybe per-player support?
  }

  @Override
  public void close() {
    INSTANCES.remove(this.key);
    this.plugin.getProxy().getPluginManager().unregisterListener(this.listener);
    this.bossBars.hideAll();
    super.close();
  }
}
