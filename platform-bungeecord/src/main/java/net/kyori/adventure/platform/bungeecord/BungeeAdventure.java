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
import net.kyori.adventure.platform.facet.Knob;
import net.kyori.adventure.text.serializer.bungeecord.BungeeCordComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;

import static net.kyori.adventure.platform.facet.Knob.logError;

/**
 * Adventure for BungeeCord.
 *
 * @see #of(Plugin)
 */
public final class BungeeAdventure {
  private BungeeAdventure() {
  }

  static {
    Knob.OUT = message -> ProxyServer.getInstance().getLogger().log(Level.INFO, message);
    Knob.ERR = (message, error) -> ProxyServer.getInstance().getLogger().log(Level.WARNING, message, error);

    // Inject our adapter component into Bungee's Gson instance
    // (this is separate from the ComponentSerializer Gson instance)
    // Please, just use Velocity!
    try {
      final Field gsonField = ProxyServer.getInstance().getClass().getDeclaredField("gson");
      gsonField.setAccessible(true);
      final Gson gson = (Gson) gsonField.get(ProxyServer.getInstance());
      BungeeCordComponentSerializer.inject(gson);
    } catch(final Throwable error) {
      logError(error, "Failed to inject ProxyServer gson");
    }
  }

  private static final Map<Plugin, BungeeAudienceProvider> INSTANCES = Collections.synchronizedMap(new IdentityHashMap<>(4));

  /**
   * Gets the audience provider.
   *
   * @param plugin a plugin
   * @return the audience provider
   */
  public static BungeeAudienceProvider of(final Plugin plugin) {
    return INSTANCES.computeIfAbsent(plugin, BungeeAudienceProviderImpl::new);
  }
}
