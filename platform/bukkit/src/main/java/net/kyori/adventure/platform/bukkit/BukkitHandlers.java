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

import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/* package */ class BukkitHandlers {
  private BukkitHandlers() {}

  /* package */ static String legacy(final @NonNull Component component) {
      return BukkitPlatform.LEGACY_SERIALIZER.serialize(component);
  }

  /* package */ static class Chat implements Handler.Chat<CommandSender, String> {
    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    public String initState(final @NonNull Component component) {
      return legacy(component);
    }

    @Override
    public void send(final @NonNull CommandSender target, final @NonNull String message) {
      target.sendMessage(message);
    }
  }

  /* package */ static class BossBarNameSetter implements BukkitBossBarListener.NameSetter {

    @Override
    public void setName(final org.bukkit.boss.@NonNull BossBar bar, final @NonNull Component name) {
      bar.setTitle(legacy(name));
    }

    @Override
    public boolean isAvailable() {
      return BukkitBossBarListener.SUPPORTED;
    }
  }

  /* package */ static abstract class PlaySound implements Handler.PlaySound<Player> {
    private static final boolean IS_AT_LEAST_113 = Crafty.hasClass("org.bukkit.NamespacedKey");

    @Override
    public void play(final @NonNull Player viewer, final @NonNull Sound sound) {
      play(viewer, sound, viewer.getLocation());
    }

    @Override
    public void play(final @NonNull Player viewer, final @NonNull Sound sound, final double x, final double y, final double z) {
      play(viewer, sound, new Location(viewer.getWorld(), x, y, z));
    }
    
    protected abstract void play(final @NonNull Player viewer, final @NonNull Sound sound, final @NonNull Location position);

    static @NonNull String name(final @Nullable Key name) {
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
  
  /* package */ static class PlaySound_WithCategory extends PlaySound {
    private static final boolean SOUND_CATEGORY_SUPPORTED = Crafty.hasMethod(Player.class, "stopSound", String.class, Crafty.findClass("org.bukkit.SoundCategory")); // Added MC 1.11

    @Override
    public boolean isAvailable() {
      return SOUND_CATEGORY_SUPPORTED;
    }

    @Override
    protected void play(final @NonNull Player viewer, final @NonNull Sound sound, final @NonNull Location position) {
      final String name = name(sound.name());
      final SoundCategory category = category(sound.source());
      viewer.playSound(position, name, category, sound.volume(), sound.pitch());
    }

    @Override
    public void stop(final @NonNull Player viewer, final @NonNull SoundStop stop) {
      final String soundName = name(stop.sound());
      final Sound./* @Nullable */ Source source = stop.source();
      final SoundCategory category = source == null ? null : category(source);
      viewer.stopSound(soundName, category);
    }

    private static SoundCategory category(final Sound.@NonNull Source source) {
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
  
  /* package */ static class PlaySound_NoCategory extends PlaySound {
    private static final boolean SOUND_STOP_SUPPORTED = Crafty.hasMethod(Player.class, "stopSound", String.class); // Added MC 1.9

    @Override
    public boolean isAvailable() {
      return true;
    }

    @Override
    protected void play(final @NonNull Player viewer, final @NonNull Sound sound, final @NonNull Location position) {
      viewer.playSound(position, name(sound.name()), sound.volume(), sound.pitch());
    }

    @Override
    public void stop(final @NonNull Player viewer, final @NonNull SoundStop sound) {
      if(!SOUND_STOP_SUPPORTED) {
        return;
      }
      viewer.stopSound(name(sound.sound()));
    }
  }
}
