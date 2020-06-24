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
package net.kyori.adventure.platform.bukkit;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.MutableGraph;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.impl.AdventurePlatformImpl;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import net.kyori.adventure.platform.impl.JdkLogHandler;
import net.kyori.adventure.platform.impl.Knobs;
import net.kyori.adventure.text.serializer.VersionedGsonComponentSerializer;
import net.kyori.adventure.platform.viaversion.ViaVersionHandlers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import us.myles.ViaVersion.api.platform.ViaPlatform;

import static java.util.Objects.requireNonNull;

public final class BukkitPlatform extends AdventurePlatformImpl implements Listener {

  private static final Map<String, BukkitPlatform> INSTANCES = new ConcurrentHashMap<>();
  private static final String PLUGIN_VIAVERSION = "ViaVersion";


  // A derivative of the Gson serializer that will serialize text appropriately based on the server version
  /* package */ static final VersionedGsonComponentSerializer GSON_SERIALIZER;

  static {
    Knobs.logger(new JdkLogHandler());
    if(Crafty.enumValue(Material.class, "NETHERITE_PICKAXE") != null) { // we are 1.16
      GSON_SERIALIZER = VersionedGsonComponentSerializer.MODERN;
    } else {
      GSON_SERIALIZER = VersionedGsonComponentSerializer.PRE_1_16;
    }
  }

  /**
   * Add the provided plugin as a softdepend of ourselves.
   *
   * <p>This removes the PluginClassLoader warning added by Spigot without
   * requiring every user to add ViaVersion to their own plugin.yml.</p>
   *
   * <p>We do assume here that each copy of Adventure belongs to a JavaPlugin.
   * If that is not true, we will silently fail to inject.</p>
   *
   * <p>If we are a {@link JavaPlugin}, we attempt to log if any errors occur at a debug level.</p>
   *
   *
   * @param pluginName plugin to add
   */
  @SuppressWarnings("unchecked")
  private static void injectSoftdepend(final @NonNull Plugin plugin, final @NonNull String pluginName) { // begone, warnings!
    try {
      //final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(BukkitPlatform.class);

      PluginDescriptionFile pdf = plugin.getDescription();
      if(pdf.getName().equals(pluginName)) return; // don't depend on ourselves?

      final Field softdepend = Crafty.field(pdf.getClass(), "softDepend");

      final List<String> dependencies = (List<String>) softdepend.get(pdf);
      if(!dependencies.contains(pluginName)) { // add to plugin.yml
        final List<String> newList = ImmutableList.<String>builder().addAll(dependencies).add(pluginName).build();
        softdepend.set(pdf, newList);
      }

      // add to dependency graph (added 14c9d275141 during MC 1.15).
      // even if this fails (on older versions), we'll still have added ourselves to the PluginDescriptionFile
      PluginManager manager = plugin.getServer().getPluginManager();
      final Field dependencyGraphField = Crafty.field(manager.getClass(), "dependencyGraph");
      final MutableGraph<String> graph = (MutableGraph<String>) dependencyGraphField.get(manager);
      graph.putEdge(pdf.getName(), pluginName);
    } catch(Throwable error) { // fail silently
      Knobs.logError("injecting soft-dependency", error);
    }
  }

  public static BukkitPlatform of(final @NonNull Plugin plugin) {
    final String key = plugin.getDescription().getName().toLowerCase(Locale.ROOT);
    BukkitPlatform platform = INSTANCES.get(key);
    if(platform == null) {
      platform = new BukkitPlatform(plugin);
      final BukkitPlatform existing = INSTANCES.putIfAbsent(key, platform);
      if(existing != null) {
        return existing;
      }
      platform.init();
    }
    return platform;
  }

  private final Plugin plugin;
  private final PhantomEntityTracker entityTracker;
  private final BukkitViaProvider viaProvider;
  private final HandlerCollection<? super CommandSender, ? extends Handler.Chat<? super CommandSender, ?>> chat;
  private final HandlerCollection<Player, Handler.ActionBar<Player, ?>> actionBar;
  private final HandlerCollection<Player, Handler.Titles<Player>> title;
  private final HandlerCollection<Player, Handler.BossBars<Player>> bossBar;
  private final HandlerCollection<Player, Handler.PlaySound<Player>> playSound;
  private final HandlerCollection<Player, Handler.Books<Player>> books;

  public BukkitPlatform(final @NonNull Plugin plugin) {
    this.plugin = requireNonNull(plugin, "plugin");
    this.entityTracker = new PhantomEntityTracker(plugin);
    injectSoftdepend(this.plugin, "ViaVersion");
    this.viaProvider = new BukkitViaProvider(this.plugin.getServer().getPluginManager());

    this.chat = HandlerCollection.of(
      new ViaVersionHandlers.Chat<>(this.viaProvider),
      new SpigotHandlers.Chat(),
      new SpigotHandlers.Chat_PlayerOnly(),
      new CraftBukkitHandlers.Chat(),
      new BukkitHandlers.Chat());
    this.actionBar = HandlerCollection.of(
      new ViaVersionHandlers.ActionBar<>(this.viaProvider),
      new SpigotHandlers.ActionBar(),
      new CraftBukkitHandlers.ActionBarModern(),
      new CraftBukkitHandlers.ActionBar1_8thru1_11());
    this.title = HandlerCollection.of(
      new ViaVersionHandlers.Titles<>(this.viaProvider),
      new PaperHandlers.Titles(),
      new CraftBukkitHandlers.Titles());
    this.bossBar = HandlerCollection.of(
      new ViaVersionHandlers.BossBars<>(this.viaProvider),
      new BukkitBossBarListener(),
      new CraftBukkitHandlers.BossBars_1_8(this.entityTracker));
    this.playSound = HandlerCollection.of(
      new BukkitHandlers.PlaySound_WithCategory(),
      new ViaVersionHandlers.PlaySound<>(this.viaProvider, player -> {
        final Location pos = player.getLocation();
        return new ViaVersionHandlers.PlaySound.Pos(pos.getX(), pos.getY(), pos.getZ());
      }),
      new BukkitHandlers.PlaySound_NoCategory());
    this.books = HandlerCollection.of(
      new SpigotHandlers.OpenBook(),
      new CraftBukkitHandlers.Books(),
      new CraftBukkitHandlers.Books_Pre1_13()
    );
  }

  /**
   * Register a single event as a callback function attached to this platform.
   *
   * <p>Cancelled events will be ignored.</p>
   *
   * @param type Event type
   * @param priority priority to listen at
   * @param handler callback fnuction
   * @param <T> event type
   */
  @SuppressWarnings("unchecked")
  private <T extends Event> void registerEvent(final Class<T> type, final EventPriority priority, final Consumer<T> handler) {
    requireNonNull(handler, "handler");
    this.plugin.getServer().getPluginManager().registerEvent(type, this, priority, (listener, event) -> handler.accept((T) event), this.plugin, true);
  }

  private void init() {
    registerEvent(PlayerJoinEvent.class, EventPriority.LOWEST, event -> {
      this.add(new BukkitPlayerAudience(event.getPlayer(), chat, actionBar, title, bossBar, playSound, books));
    });
    registerEvent(PlayerQuitEvent.class, EventPriority.MONITOR, event -> {
      this.remove(event.getPlayer().getUniqueId());
      for(Handler.BossBars<Player> handler : this.bossBar) {
        handler.hideAll(event.getPlayer());
      }
    });
    
    // ViaVersion
    registerEvent(PluginEnableEvent.class, EventPriority.NORMAL, event -> {
      if(event.getPlugin().getName().equals(PLUGIN_VIAVERSION)) {
        this.viaProvider.platform(); // init
      }
    });
    registerEvent(PluginDisableEvent.class, EventPriority.NORMAL, event -> {
      if(event.getPlugin().getName().equals(PLUGIN_VIAVERSION)) {
        this.viaProvider.dirtyVia();
      }
    });

    this.add(new BukkitSenderAudience<>(this.plugin.getServer().getConsoleSender(), this.chat, null, null, null, null, null));
  }

  public @NonNull Audience player(final @NonNull Player player) {
    return player(requireNonNull(player, "player").getUniqueId());
  }

  public @NonNull Audience audience(final @NonNull CommandSender sender) {
    requireNonNull(sender, "sender");

    if(sender instanceof Player) {
      return player(((Player) sender).getUniqueId());
    } else if(sender instanceof ConsoleCommandSender) {
      return console();
    } else {
      return new BukkitSenderAudience<>(sender, this.chat, null, null, null, null, null);
    }
  }

  @Override
  public void close() {
    HandlerList.unregisterAll(this);
    for(Handler.BossBars<Player> handler : this.bossBar) {
      handler.hideAll();
    }
  }

  /* package */ static class BukkitViaProvider implements ViaVersionHandlers.ViaAPIProvider<CommandSender> {

    private final PluginManager plugins;
    private volatile ViaPlatform<Player> platform = null;

    /* package */ BukkitViaProvider(final @NonNull PluginManager plugins) {
      this.plugins = plugins;
    }

    @Override
    public boolean isAvailable() {
      try {
        final Class<?> apiKlass = Crafty.findClass("us.myles.ViaVersion.api.ViaAPI");
        if(apiKlass == null) {
          return false;
        }
        final Plugin owningPlugin = JavaPlugin.getProvidingPlugin(apiKlass);
        if(owningPlugin != null && owningPlugin.getName().equals(PLUGIN_VIAVERSION)) {
          return true;
        }
      } catch(Exception error) {
        Knobs.logError("detecting ViaVersion", error);
      }
      return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ViaPlatform<Player> platform() {
      ViaPlatform<Player> platform = this.platform;
      if(platform == null) {
        this.platform = platform = (ViaPlatform<Player>) this.plugins.getPlugin(PLUGIN_VIAVERSION);
      }
      return platform;
    }

    @Override
    public UUID id(final @NonNull CommandSender viewer) {
      if(!(viewer instanceof Player)) {
        return null;
      }

      return ((Player) viewer).getUniqueId();
    }

    @Override
    public @NonNull VersionedGsonComponentSerializer serializer(final @NonNull CommandSender viewer) {
      if(isAvailable()) {
        return ViaVersionHandlers.ViaAPIProvider.super.serializer(viewer);
      }
      return BukkitPlatform.GSON_SERIALIZER;
    }

    /* package */ void dirtyVia() {
      this.platform = null;
    }
  }

}
