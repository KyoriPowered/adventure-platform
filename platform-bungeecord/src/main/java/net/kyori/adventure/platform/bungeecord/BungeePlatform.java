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

import java.lang.reflect.Field;
import net.kyori.adventure.platform.impl.AdventurePlatformImpl;
import net.kyori.adventure.platform.impl.JdkLogHandler;
import net.kyori.adventure.platform.impl.Knobs;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventBus;
import net.md_5.bungee.event.EventHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;

public class BungeePlatform extends AdventurePlatformImpl implements Listener {
  
  static {
    Knobs.logger(new JdkLogHandler());
  }

  private final ProxyServer proxy;

  public BungeePlatform() {
    this(ProxyServer.getInstance());
  }

  BungeePlatform(final @NonNull ProxyServer proxy) {
    this.proxy = requireNonNull(proxy, "proxy");
    
    final EventBus bus = eventBus(proxy);
    if(bus != null) {
      bus.register(this);
    } else {
      try {
        this.proxy.getPluginManager().registerListener(new Plugin() {}, this);
      } catch(Exception ex) {
        Knobs.logError("registering events with fake plugin", ex);
      }
    }
  }

  @EventHandler(priority = Byte.MIN_VALUE /* before EventPriority.LOWEST */)
  public void onLogin(PostLoginEvent event) {
    this.add(new BungeePlayerAudience(proxy, event.getPlayer()));
  }

  @EventHandler(priority = Byte.MAX_VALUE /* after EventPriority.HIGHEST */)
  public void onQuit(PlayerDisconnectEvent event) {
    this.remove(event.getPlayer().getUniqueId());
  }

  /**
   * Attempt to access the server's event bus, so we can register events without a Plugin instance
   * @param server server to get bus from
   * @return the event bus, or null if unable to retrieve
   */
  private static @Nullable EventBus eventBus(final @NonNull ProxyServer server) {
    final PluginManager mgr = server.getPluginManager();
    try {
      final Field eventBusField = mgr.getClass().getDeclaredField("eventBus");
      eventBusField.setAccessible(true);
      return (EventBus) eventBusField.get(mgr);
    } catch(NoSuchFieldException | IllegalAccessException ex) {
      Knobs.logError("getting Bungee event bus", ex);
      return null;
    }
  }
  
}
