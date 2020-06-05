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

import java.lang.reflect.Proxy;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.AdventurePlatform;
import net.kyori.adventure.platform.PlatformAudience;
import net.kyori.adventure.platform.ProviderSupport;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;

public final class CraftBukkitPlatform implements AdventurePlatform {
  static final boolean BOSS_BAR_SUPPORTED;
  static final boolean SOUND_CATEGORY_SUPPORTED;
  static final boolean SOUND_STOP_SUPPORTED;
  static final boolean IS_AT_LEAST_113;
  static final BukkitBossBarListener BOSS_BARS = new BukkitBossBarListener();

  static {
    BOSS_BAR_SUPPORTED = Crafty.hasClass("org.bukkit.boss.BossBar"); // Added MC 1.9
    SOUND_CATEGORY_SUPPORTED = Crafty.hasMethod(Player.class, "stopSound", String.class, Crafty.findClass("org.bukkit.SoundCategory")); // Added MC 1.11
    SOUND_STOP_SUPPORTED = Crafty.hasMethod(Player.class, "stopSound", String.class); // Added MC 1.9
    IS_AT_LEAST_113 = Crafty.hasClass("org.bukkit.NamespacedKey");
    final Plugin fakePlugin = (Plugin) Proxy.newProxyInstance(CraftBukkitPlatform.class.getClassLoader(), new Class[] {Plugin.class}, (proxy, method, args) -> {
      switch(method.getName()) {
        case "isEnabled":
          return true;
        case "equals":
          return proxy == args[0];
        default:
          return null; // yeet
      }
    });
    final Listener holder = new Listener() {};
    
    // Remove players from boss bars
    Bukkit.getPluginManager().registerEvent(PlayerQuitEvent.class, holder, EventPriority.NORMAL, (listener, event) -> {
      BOSS_BARS.unsubscribeFromAll(((PlayerQuitEvent) event).getPlayer());
    }, fakePlugin, false);
  }

  public CraftBukkitPlatform() {}

  private final PlatformAudience<ConsoleCommandSender> console = new ConsoleAudience(Bukkit.getConsoleSender());

  // TODO: ugly but it's here to test with until proper solution
  public static PlatformAudience<Player> audience(final Player player) {
    return new PlayerAudience(player);
  }

  public static PlatformAudience<? extends CommandSender> audience(final CommandSender sender) {
    return BukkitAudience.of(sender);
  }

  @Override
  public @NonNull String name() {
    return "CraftBukkit";
  }

  @Override
  public @NonNull ProviderSupport supportLevel() {
    return ProviderSupport.FULL;
  }

  @Override
  public @NonNull PlatformAudience<ConsoleCommandSender> console() {
    return console;
  }

  @Override
  public @NonNull MultiAudience audience(final @NonNull Iterable<Audience> audiences) {
    return MultiAudience.of(audiences);
  }

  @Override
  public @NonNull MultiAudience permission(final @NonNull String permission) {
    return new WithPermissionAudience(Bukkit.getServer(), requireNonNull(permission, "permission"));
  }

  @Override
  public @NonNull MultiAudience online() {
    return new OnlinePlayersAudience(Bukkit.getServer());
  }

  static SoundCategory category(final Sound.@NonNull Source source) {
    switch(source) {
      case MASTER: return SoundCategory.MASTER;
      case MUSIC: return SoundCategory.MUSIC;
      case RECORD: return SoundCategory.RECORDS;
      case WEATHER: return SoundCategory.WEATHER;
      case BLOCK: return SoundCategory.BLOCKS;
      case HOSTILE: return SoundCategory.HOSTILE;
      case NEUTRAL: return SoundCategory.NEUTRAL;
      case PLAYER: return SoundCategory.PLAYERS;
      case AMBIENT: return SoundCategory.AMBIENT;
      case VOICE: return SoundCategory.VOICE;
      default: throw new IllegalArgumentException("Unknown sound source " + source);
    }
  }
  
  static @NonNull String soundName(final @Nullable Key name) {
    if(name == null) {
      return "";
    }

    if(IS_AT_LEAST_113) { // sound format changed to use identifiers
      return name.asString();
    } else {
      return name.value();
    }
  }
}
