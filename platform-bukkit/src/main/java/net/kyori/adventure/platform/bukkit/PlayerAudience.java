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

import java.util.Collections;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

class PlayerAudience extends BukkitAudience<Player> {
  public PlayerAudience(final Player player) {
    super(requireNonNull(player, "player"));
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    if (CraftBukkitPlatform.BOSS_BAR_SUPPORTED) {
      CraftBukkitPlatform.BOSS_BARS.subscribe(this.viewer, requireNonNull(bar, "bar"));
    }
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    if (CraftBukkitPlatform.BOSS_BAR_SUPPORTED) {
      CraftBukkitPlatform.BOSS_BARS.unsubscribe(this.viewer, requireNonNull(bar, "bar"));
    }
  }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    TextAdapter0.sendComponent(Collections.singleton(this.viewer), message, true);
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    playSound(sound, this.viewer.getLocation());
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    playSound(sound, new Location(this.viewer.getWorld(), x, y, z));
  }

  private void playSound(final @NonNull Sound sound, final @NonNull Location loc) {
    final String name = CraftBukkitPlatform.soundName(sound.name());
    if(CraftBukkitPlatform.SOUND_CATEGORY_SUPPORTED) {
      final SoundCategory category = CraftBukkitPlatform.category(sound.source());
      this.viewer.playSound(loc, name, category, sound.volume(), sound.pitch());
    } else {
      this.viewer.playSound(loc, name, sound.volume(), sound.pitch());
    }
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    if(!CraftBukkitPlatform.SOUND_STOP_SUPPORTED) {
      return;
    }

    final String soundName = CraftBukkitPlatform.soundName(stop.sound());
    if(CraftBukkitPlatform.SOUND_CATEGORY_SUPPORTED) {
      final Sound.Source source = stop.source();
      final SoundCategory category = source == null ? null : CraftBukkitPlatform.category(source);
      this.viewer.stopSound(soundName, category);
    } else {
      this.viewer.stopSound(soundName);
    }
  }

  @Override
  public void showTitle(final @NonNull Title title) {
    // (TITLE, text)
    // (SUBTITLE, text)
    // (fadeIn, stay, fadeOut)
  }

  @Override
  public void clearTitle() {
    this.viewer.sendTitle("", "", -1, -1, -1);
  }

  @Override
  public void resetTitle() {
    this.viewer.resetTitle();
  }
}
