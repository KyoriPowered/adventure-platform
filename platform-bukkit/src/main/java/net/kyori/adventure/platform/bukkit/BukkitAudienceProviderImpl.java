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
package net.kyori.adventure.platform.bukkit;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.MutableGraph;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.facet.FacetAudienceProvider;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.platform.facet.Knob.logError;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findClass;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.findMethod;
import static net.kyori.adventure.text.serializer.craftbukkit.MinecraftReflection.needField;

@SuppressWarnings("unchecked")
/* package */ final class BukkitAudienceProviderImpl extends FacetAudienceProvider<CommandSender, BukkitAudience> implements BukkitAudienceProvider, Listener {
  private static final String VIAVERSION = "ViaVersion";

  private static final @Nullable Class<? extends PlayerEvent> LOCALE_CHANGE_EVENT;
  private static final @Nullable MethodHandle LOCALE_CHANGE_EVENT_GETTER;

  static {
    Class<?> localeEvent = findClass("org.bukkit.event.player.PlayerLocaleChangeEvent");
    if(localeEvent == null) {
      localeEvent = findClass("com.destroystokyo.paper.event.player.PlayerLocaleChangeEvent");
    }

    MethodHandle localeGetter = findMethod(localeEvent, "getLocale", String.class);
    if(localeGetter == null) {
      localeGetter = findMethod(localeEvent, "getNewLocale", String.class);
    }

    LOCALE_CHANGE_EVENT = localeEvent == null ? null : (Class<? extends PlayerEvent>) localeEvent;
    LOCALE_CHANGE_EVENT_GETTER = localeGetter;
  }

  private final Plugin plugin;

  /* package */ BukkitAudienceProviderImpl(final @NonNull Plugin plugin) {
    this.plugin = requireNonNull(plugin, "plugin");
    softDepend(this.plugin, VIAVERSION);

    this.addViewer(this.plugin.getServer().getConsoleSender());
    for(final Player player : this.plugin.getServer().getOnlinePlayers()) {
      this.addViewer(player);
    }

    this.registerEvent(PlayerJoinEvent.class, EventPriority.LOWEST, event ->
      this.addViewer(event.getPlayer()));
    this.registerEvent(PlayerQuitEvent.class, EventPriority.MONITOR, event ->
      this.removeViewer(event.getPlayer()));

    if(LOCALE_CHANGE_EVENT_GETTER != null) {
      this.registerEvent(LOCALE_CHANGE_EVENT, EventPriority.MONITOR, event -> {
        try {
          this.changeViewer(event.getPlayer(), toLocale((String) LOCALE_CHANGE_EVENT_GETTER.invoke(event)));
        } catch(final Throwable error) {
          logError(error, "Failed to change locale: %s", event.getPlayer());
        }
      });
    }
  }

  @Override
  public Audience sender(final @NonNull CommandSender sender) {
    if(sender instanceof Player) {
      return this.player((Player) sender);
    }
    if(sender instanceof ConsoleCommandSender) {
      return this.console();
    }
    if(sender instanceof ProxiedCommandSender) {
      return this.sender(((ProxiedCommandSender) sender).getCallee());
    }
    if(sender instanceof Entity || sender instanceof Block) {
      return Audience.empty();
    }
    return this.createAudience(Collections.singletonList(sender));
  }

  @Override
  public Audience player(final @NonNull Player player) {
    return this.player(player.getUniqueId());
  }

  @Override
  protected @Nullable UUID hasId(final @NonNull CommandSender viewer) {
    if(viewer instanceof Player) {
      return ((Player) viewer).getUniqueId();
    }
    return null;
  }

  @Override
  protected boolean isConsole(final @NonNull CommandSender viewer) {
    return viewer instanceof ConsoleCommandSender;
  }

  @Override
  protected boolean hasPermission(final @NonNull CommandSender viewer, final @NonNull String permission) {
    return viewer.hasPermission(permission);
  }

  @Override
  protected boolean isInWorld(final @NonNull CommandSender viewer, final @NonNull Key world) {
    if(viewer instanceof Player) {
      return ((Player) viewer).getWorld().getName().equals(world.value());
    }
    return false;
  }

  @Override
  protected boolean isOnServer(final @NonNull CommandSender viewer, final @NonNull String server) {
    return false;
  }

  @Override
  protected BukkitAudience createAudience(final @NonNull Collection<CommandSender> viewers) {
    return new BukkitAudience(this.plugin, viewers, null);
  }

  /**
   * Register a single event as a callback function attached to this platform.
   *
   * <p>Cancelled events will be ignored.</p>
   *
   * @param type event type
   * @param priority priority to listen at
   * @param handler callback function
   * @param <T> event type
   */
  @SuppressWarnings("unchecked")
  private <T extends Event> void registerEvent(final Class<T> type, final EventPriority priority, final Consumer<T> handler) {
    requireNonNull(handler, "handler");
    this.plugin.getServer().getPluginManager().registerEvent(type, this, priority, (listener, event) -> handler.accept((T) event), this.plugin, true);
  }

  /**
   * Add the provided plugin as a soft-depend of ourselves.
   *
   * <p>This removes the PluginClassLoader warning added by Spigot without
   * requiring every user to add ViaVersion to their own plugin.yml.</p>
   *
   * <p>We do assume here that each copy of Adventure belongs to a JavaPlugin.
   * If that is not true, we will silently fail to inject.</p>
   *
   * @param pluginName plugin to add
   */
  @SuppressWarnings("unchecked")
  private static void softDepend(final @NonNull Plugin plugin, final @NonNull String pluginName) {
    try {
      final PluginDescriptionFile pdf = plugin.getDescription();
      if(pdf.getName().equals(pluginName)) return;

      final Field softdepend = needField(pdf.getClass(), "softDepend");
      final List<String> dependencies = (List<String>) softdepend.get(pdf);
      if(!dependencies.contains(pluginName)) { // Add to plugin.yml
        final List<String> newList = ImmutableList.<String>builder().addAll(dependencies).add(pluginName).build();
        softdepend.set(pdf, newList);
      }

      // Add to dependency graph (added 14c9d275141 during MC 1.15).
      // Even if this fails (on older versions), we'll still have added ourselves to the PluginDescriptionFile
      final PluginManager manager = plugin.getServer().getPluginManager();
      final Field dependencyGraphField = needField(manager.getClass(), "dependencyGraph");
      final MutableGraph<String> graph = (MutableGraph<String>) dependencyGraphField.get(manager);
      graph.putEdge(pdf.getName(), pluginName);
    } catch(final Throwable error) {
      // No-op, likely on a version without a dependency graph
    }
  }

  /**
   * Take a locale string provided from a minecraft client and attempt to parse it as a locale.
   * These are not strictly compliant with the iso standard, so we try to make things a bit more normalized.
   *
   * @param locale a locale string, in the format provided by the Minecraft client
   * @return a locale
   */
  private static @NonNull Locale toLocale(final @Nullable String locale) {
    if(locale == null) return Locale.US;
    final String[] parts = locale.split("_", 3);
    switch(parts.length) {
      case 1: return parts[0].isEmpty() ? Locale.US : new Locale(parts[0]);
      case 2: return new Locale(parts[0], parts[1]);
      case 3: return new Locale(parts[0], parts[1], parts[2]);
      default: return Locale.US;
    }
  }
}
