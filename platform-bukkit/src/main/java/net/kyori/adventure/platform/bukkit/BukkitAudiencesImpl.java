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
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.facet.FacetAudienceProvider;
import net.kyori.adventure.platform.facet.Knob;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.bukkit.Bukkit;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findMethod;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.needField;
import static net.kyori.adventure.platform.facet.Knob.logError;

@SuppressWarnings("unchecked")
final class BukkitAudiencesImpl extends FacetAudienceProvider<CommandSender, BukkitAudience> implements BukkitAudiences, Listener {

  static {
    Knob.OUT = message -> Bukkit.getLogger().log(Level.INFO, message);
    Knob.ERR = (message, error) -> Bukkit.getLogger().log(Level.WARNING, message, error);
  }

  private static final Map<String, BukkitAudiences> INSTANCES = Collections.synchronizedMap(new HashMap<>(4));

  static Builder builder(final @NotNull Plugin plugin) {
    return new Builder(plugin);
  }

  static BukkitAudiences instanceFor(final @NotNull Plugin plugin) {
    return builder(plugin).build();
  }

  private final Plugin plugin;

  BukkitAudiencesImpl(final @NotNull Plugin plugin, final @NotNull ComponentRenderer<Pointered> componentRenderer) {
    super(componentRenderer);
    this.plugin = requireNonNull(plugin, "plugin");
    this.softDepend("ViaVersion");

    final CommandSender console = this.plugin.getServer().getConsoleSender();
    this.addViewer(console);

    for (final Player player : this.plugin.getServer().getOnlinePlayers()) {
      this.addViewer(player);
    }

    this.registerEvent(PlayerJoinEvent.class, EventPriority.LOWEST, event ->
      this.addViewer(event.getPlayer()));
    this.registerEvent(PlayerQuitEvent.class, EventPriority.MONITOR, event ->
      this.removeViewer(event.getPlayer()));
    this.registerLocaleEvent(EventPriority.MONITOR, (viewer, locale) -> {
      final @Nullable BukkitAudience audience = this.viewers.get(viewer);
      if (audience != null) audience.locale(locale);
    });
  }

  @Override
  public @NotNull Audience sender(final @NotNull CommandSender sender) {
    if (sender instanceof Player) {
      return this.player((Player) sender);
    } else if (sender instanceof ConsoleCommandSender) {
      return this.console();
    } else if (sender instanceof ProxiedCommandSender) {
      return this.sender(((ProxiedCommandSender) sender).getCallee());
    } else if (sender instanceof Entity || sender instanceof Block) {
      return Audience.empty();
    }
    return this.createAudience(Collections.singletonList(sender));
  }

  @Override
  public @NotNull Audience player(final @NotNull Player player) {
    return this.player(player.getUniqueId());
  }

  @Override
  protected @NotNull BukkitAudience createAudience(final @NotNull Collection<CommandSender> viewers) {
    return new BukkitAudience(this.plugin, this, viewers);
  }

  @Override
  public @NotNull ComponentFlattener flattener() {
    return BukkitComponentSerializer.FLATTENER;
  }

  static final class Builder implements BukkitAudiences.Builder {
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
    public BukkitAudiences.@NotNull Builder partition(final @NotNull Function<Pointered, ?> partitionFunction) {
      requireNonNull(partitionFunction, "partitionFunction"); // unused
      return this;
    }

    @Override
    public @NotNull BukkitAudiences build() {
      return INSTANCES.computeIfAbsent(this.plugin.getName(), name -> new BukkitAudiencesImpl(this.plugin, this.componentRenderer));
    }
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
   * @param pluginName a plugin name
   */
  @SuppressWarnings("unchecked")
  private void softDepend(final @NotNull String pluginName) {
    final PluginDescriptionFile file = this.plugin.getDescription();
    if (file.getName().equals(pluginName)) return;

    try {
      final Field softDepend = needField(file.getClass(), "softDepend");
      final List<String> dependencies = (List<String>) softDepend.get(file);
      if (!dependencies.contains(pluginName)) {
        final List<String> newList = ImmutableList.<String>builder().addAll(dependencies).add(pluginName).build();
        softDepend.set(file, newList);
      }
    } catch (final Throwable error) {
      logError(error, "Failed to inject softDepend in plugin.yml: %s %s", this.plugin, pluginName);
    }

    try {
      final PluginManager manager = this.plugin.getServer().getPluginManager();
      final Field dependencyGraphField = needField(manager.getClass(), "dependencyGraph");
      final MutableGraph<String> graph = (MutableGraph<String>) dependencyGraphField.get(manager);
      graph.putEdge(file.getName(), pluginName);
    } catch (final Throwable error) {
      // Fail silently, dependency graphs were added in 1.15, but the previous method still works
    }
  }

  /**
   * Registers an event listener as a callback.
   *
   * <p>Cancelled events will be ignored.</p>
   *
   * @param type an event type
   * @param priority a listener priority
   * @param callback a callback
   * @param <T> an event type
   */
  @SuppressWarnings("unchecked")
  private <T extends Event> void registerEvent(final @NotNull Class<T> type, final @NotNull EventPriority priority, final @NotNull Consumer<T> callback) {
    requireNonNull(callback, "callback");
    this.plugin.getServer().getPluginManager().registerEvent(type, this, priority, (listener, event) -> callback.accept((T) event), this.plugin, true);
  }

  /**
   * Registers a callback to listen for {@code PlayerLocaleChangeEvent}.
   *
   * <p>Bukkit has history of multiple versions of this event, so some
   * reflection work is needed to detect the right one.</p>
   *
   * @param priority a priority
   * @param callback a callback
   */
  private void registerLocaleEvent(final EventPriority priority, final @NotNull BiConsumer<Player, Locale> callback) {
    Class<?> eventClass = findClass("org.bukkit.event.player.PlayerLocaleChangeEvent");
    if (eventClass == null) {
      eventClass = findClass("com.destroystokyo.paper.event.player.PlayerLocaleChangeEvent");
    }

    MethodHandle getMethod = findMethod(eventClass, "getLocale", String.class);
    if (getMethod == null) {
      getMethod = findMethod(eventClass, "getNewLocale", String.class);
    }

    if (getMethod != null && PlayerEvent.class.isAssignableFrom(eventClass)) {
      final Class<? extends PlayerEvent> localeEvent = (Class<? extends PlayerEvent>) eventClass;
      final MethodHandle getLocale = getMethod;

      this.registerEvent(localeEvent, priority, event -> {
        final Player player = event.getPlayer();
        final String locale;
        try {
          locale = (String) getLocale.invoke(event);
        } catch (final Throwable error) {
          logError(error, "Failed to accept %s: %s", localeEvent.getName(), player);
          return;
        }
        callback.accept(player, BukkitAudiencesImpl.toLocale(locale));
      });
    }
  }

  /**
   * Converts a raw locale given by the client to a nicer Locale object.
   *
   * @param string a raw locale
   * @return a parsed locale
   */
  private static @NotNull Locale toLocale(final @Nullable String string) {
    if (string != null) {
      final Locale locale = Translator.parseLocale(string);
      if (locale != null) {
        return locale;
      }
    }
    return Locale.US;
  }
}
