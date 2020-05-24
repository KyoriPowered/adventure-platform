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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

public class BukkitAudience implements Audience {
  private final Player player;

  public BukkitAudience(final Player player) {
    this.player = requireNonNull(player, "player");
  }

  @Override
  public void message(@NonNull final Component message) {
    TextAdapter.sendMessage(this.player, message);
  }

  @Override
  public void showBossBar(@NonNull final BossBar bar) {
    if(!(bar instanceof BukkitBossBar)) {
      throw new IllegalArgumentException("Provided boss bar " + bar + " must be created by Adventure");
    }
    ((BukkitBossBar) bar).bukkit().addPlayer(this.player);
    // TODO: Backwards compatibility, packet + API was only added for MC 1.9
    // Use a no-op implementation of BossBar???
  }

  @Override
  public void hideBossBar(@NonNull final BossBar bar) {
    if(!(bar instanceof BukkitBossBar)) {
      throw new IllegalArgumentException("Provided boss bar " + bar + " must be created by Adventure");
    }
    ((BukkitBossBar) bar).bukkit().addPlayer(this.player);
  }

  @Override
  public void showActionBar(@NonNull final Component message) {
    TextAdapter.sendActionBar(this.player, message);
  }

  @Override
  public void playSound(@NonNull final Sound sound) {
    final String name = sound.name().toString();
    final SoundCategory category = category(sound.source());
    this.player.playSound(this.player.getLocation(), name, category, sound.volume(), sound.pitch());
    // TODO: legacy compatibility
    // Player.playSound with a SoundCategory only added MC 1.11, Bukkit 7512561bdb4c8f8f95d3dc4e5f58370437adff7f
    // SoundCategory values have not changed since addition
    // SoundCategory field has been in packet since 1.9
  }

  @Override
  public void stopSound(@NonNull final SoundStop stop) {
    final String name = stop.sound() == null ? "" : stop.sound().toString();
    final SoundCategory category = stop.source() == null ? null : category(stop.source());
    this.player.stopSound(name, category);

    // TODO: legacy compatibility
    // Player.stopSound(String) added: MC 1.9, Bukkit 32351955d81a12fa95006adb98d8c8030079248f
    // Player.stopSound(String, SoundCategory) added MC 1.11, Bukkit c1a8e12c9ce0686b527bacd40fcda6e3051f53b9
  }

  static SoundCategory category(Sound.@NonNull Source source) {
    switch(source) {
      case MASTER:
        return SoundCategory.MASTER;
      case MUSIC:
        return SoundCategory.MUSIC;
      case RECORDS:
        return SoundCategory.RECORDS;
      case WEATHER:
        return SoundCategory.WEATHER;
      case BLOCKS:
        return SoundCategory.BLOCKS;
      case HOSTILE:
        return SoundCategory.HOSTILE;
      case NEUTRAL:
        return SoundCategory.NEUTRAL;
      case PLAYERS:
        return SoundCategory.PLAYERS;
      case AMBIENT:
        return SoundCategory.AMBIENT;
      case VOICE:
        return SoundCategory.VOICE;
      default:
        throw new IllegalArgumentException("Unknown sound source " + source);
    }
  }
}
