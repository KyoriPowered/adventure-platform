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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.AdventurePlatform;
import net.kyori.adventure.platform.ProviderSupport;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

public final class CraftBukkitPlatform implements AdventurePlatform {
  private static final boolean BOSS_BAR_SUPPORTED;
  static final boolean SOUND_CATEGORY_SUPPORTED;
  static final boolean SOUND_STOP_SUPPORTED;

  static {
    BOSS_BAR_SUPPORTED = Crafty.hasClass("org.bukkit.boss.BossBar"); // Added MC 1.9
    SOUND_CATEGORY_SUPPORTED = Crafty.hasMethod(Player.class, "stopSound", String.class, Crafty.findClass("org.bukkit.SoundCategory")); // Added MC 1.11
    SOUND_STOP_SUPPORTED = Crafty.hasMethod(Player.class, "stopSound", String.class); // Added MC 1.9
  }

  public CraftBukkitPlatform() {}

  private final Audience console = new ConsoleAudience(Bukkit.getConsoleSender());

  // TODO: ugly but it's here to test with until proper solution
  public static Audience audience(final Player player) {
    return new PlayerAudience(player);
  }

  public static Audience audience(final CommandSender sender) {
    if(sender instanceof Player) {
      return new PlayerAudience((Player) sender);
    } else if(sender instanceof Audience) { // to support custom senders
      return (Audience) sender;
    } else {
      return new ConsoleAudience(sender);
    }
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
  public @NonNull Audience console() {
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

  @Override
  public @NonNull BossBar bossBar(final @NonNull Component name, final float fraction, final BossBar.@NonNull Color color, final BossBar.@NonNull Overlay overlay) {
    requireNonNull(name, "name");
    requireNonNull(color, "color");
    requireNonNull(overlay, "overlay");
    if(BOSS_BAR_SUPPORTED) {
      return new BukkitBossBar(name, fraction, color, overlay);
    } else {
      return new NoOpBossBar(name, fraction, color, overlay);
    }
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
}
