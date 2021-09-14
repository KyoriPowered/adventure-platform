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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.facet.FacetAudienceProvider;
import net.kyori.adventure.platform.facet.Knob;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.platform.facet.Knob.logError;

final class BungeeAudiencesImpl extends FacetAudienceProvider<CommandSender, BungeeAudience> implements BungeeAudiences {
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
      BungeeComponentSerializer.inject(gson);
    } catch (final Throwable error) {
      logError(error, "Failed to inject ProxyServer gson");
    }
  }

  private static final Map<String, BungeeAudiences> INSTANCES = Collections.synchronizedMap(new HashMap<>(4));

  static @NotNull BungeeAudiences instanceFor(final @NotNull Plugin plugin) {
    return builder(plugin).build();
  }

  static @NotNull Builder builder(final @NotNull Plugin plugin) {
    return new Builder(plugin);
  }

  private final Plugin plugin;
  private final Listener listener;

  BungeeAudiencesImpl(final Plugin plugin, final @NotNull ComponentRenderer<Pointered> componentRenderer) {
    super(componentRenderer);
    this.plugin = requireNonNull(plugin, "plugin");
    this.listener = new Listener();
    this.plugin.getProxy().getPluginManager().registerListener(this.plugin, this.listener);

    final CommandSender console = this.plugin.getProxy().getConsole();
    this.addViewer(console);

    for (final ProxiedPlayer player : this.plugin.getProxy().getPlayers()) {
      this.addViewer(player);
    }
  }

  @NotNull
  @Override
  public Audience sender(final @NotNull CommandSender sender) {
    if (sender instanceof ProxiedPlayer) {
      return this.player((ProxiedPlayer) sender);
    } else if (ProxyServer.getInstance().getConsole().equals(sender)) {
      return this.console();
    }
    return this.createAudience(Collections.singletonList(sender));
  }

  @NotNull
  @Override
  public Audience player(final @NotNull ProxiedPlayer player) {
    return this.player(player.getUniqueId());
  }

  @Override
  protected @NotNull BungeeAudience createAudience(final @NotNull Collection<CommandSender> viewers) {
    return new BungeeAudience(this, viewers);
  }

  @Override
  public @NotNull ComponentFlattener flattener() {
    return BungeeFacet.FLATTENER;
  }

  @Override
  public void close() {
    this.plugin.getProxy().getPluginManager().unregisterListener(this.listener);
    super.close();
  }

  static final class Builder implements BungeeAudiences.Builder {
    private final @NotNull Plugin plugin;
    private ComponentRenderer<Pointered> componentRenderer;

    Builder(final @NotNull Plugin plugin) {
      this.plugin = requireNonNull(plugin, "plugin");
      this.componentRenderer(ptr -> ptr.getOrDefault(Identity.LOCALE, DEFAULT_LOCALE), GlobalTranslator.renderer());
    }

    @Override
    public @NotNull Builder componentRenderer(final @NotNull ComponentRenderer<Pointered> componentRenderer) {
      this.componentRenderer = requireNonNull(componentRenderer, "component renderer");
      return this;
    }

    @Override
    public BungeeAudiences.@NotNull Builder partition(final @NotNull Function<Pointered, ?> partitionFunction) {
      requireNonNull(partitionFunction, "partitionFunction"); // unused
      return this;
    }

    @Override
    public @NotNull BungeeAudiences build() {
      return INSTANCES.computeIfAbsent(this.plugin.getDescription().getName(), name -> new BungeeAudiencesImpl(this.plugin, this.componentRenderer));
    }
  }

  public final class Listener implements net.md_5.bungee.api.plugin.Listener {
    @EventHandler(priority = Byte.MIN_VALUE /* before EventPriority.LOWEST */)
    public void onLogin(final PostLoginEvent event) {
      BungeeAudiencesImpl.this.addViewer(event.getPlayer());
    }

    @EventHandler(priority = Byte.MAX_VALUE /* after EventPriority.HIGHEST */)
    public void onDisconnect(final PlayerDisconnectEvent event) {
      BungeeAudiencesImpl.this.removeViewer(event.getPlayer());
    }
  }
}
