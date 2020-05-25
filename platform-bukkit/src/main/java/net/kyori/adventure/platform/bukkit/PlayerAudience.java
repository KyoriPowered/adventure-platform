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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
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
    if(this.isNoOp(bar)) {
      return;
    }
    this.ensureItIsOurs(bar);
    ((BukkitBossBar) bar).addPlayer(this.viewer);
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    this.ensureItIsOurs(bar);
    ((BukkitBossBar) bar).removePlayer(this.viewer);
  }

  private boolean isNoOp(final BossBar bar) {
    return bar instanceof NoOpBossBar;
  }

  private void ensureItIsOurs(final BossBar bar) {
    if(!(bar instanceof BukkitBossBar)) {
      throw new IllegalArgumentException("Provided boss bar " + bar + " must be created by Adventure");
    }
  }

  @Override
  public void showActionBar(final @NonNull Component message) {
    TextAdapter.sendActionBar(this.viewer, message);
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    final String name = sound.name().asString();
    if(CraftBukkitPlatform.SOUND_CATEGORY_SUPPORTED) {
      final SoundCategory category = CraftBukkitPlatform.category(sound.source());
      this.viewer.playSound(this.viewer.getLocation(), name, category, sound.volume(), sound.pitch());
    } else {
      this.viewer.playSound(this.viewer.getLocation(), name, sound.volume(), sound.pitch());
    }
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    if(!CraftBukkitPlatform.SOUND_STOP_SUPPORTED) {
      return;
    }

    final Key sound = stop.sound();
    final String name = sound == null ? "" : sound.asString();

    if(CraftBukkitPlatform.SOUND_CATEGORY_SUPPORTED) {
      final Sound.Source source = stop.source();
      final SoundCategory category = source == null ? null : CraftBukkitPlatform.category(source);
      this.viewer.stopSound(name, category);
    } else {
      this.viewer.stopSound(name);
    }
  }
}
