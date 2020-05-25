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
package net.kyori.adventure.platform.spongeapi;

import com.flowpowered.math.vector.Vector3d;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.effect.sound.SoundCategory;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.world.Locatable;

final class SpongeFullAudience extends SpongeAudience {
  private final Viewer viewer;

  public <T extends MessageReceiver & Viewer> SpongeFullAudience(T target) {
    super(target);
    this.viewer = target;
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    this.ensureItIsOurs(bar);
    if(this.viewer instanceof Player) {
      ((SpongeBossBar) bar).addPlayer((Player) this.viewer);
    }
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    this.ensureItIsOurs(bar);
    if(this.viewer instanceof Player) {
      ((SpongeBossBar) bar).removePlayer((Player) this.viewer);
    }
  }

  private void ensureItIsOurs(final BossBar bar) {
    if(!(bar instanceof SpongeBossBar)) {
      throw new IllegalArgumentException(String.format("Incompatible boss bar - expected %s, got %s", SpongeBossBar.class.getName(), bar.getClass().getName()));
    }
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    Vector3d loc = Vector3d.ZERO;
    if(this.viewer instanceof Locatable) {
      loc = ((Locatable) this.viewer).getLocation().getPosition();
    }
    final SoundType type = sponge(sound.name());
    final SoundCategory category = sponge(sound.source());
    this.viewer.playSound(type, category, loc, sound.volume(), sound.pitch());
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    final SoundType type = sponge(stop.sound());
    final SoundCategory category = sponge(stop.source());

    if(type != null && category != null) {
      this.viewer.stopSounds(type, category);
    } else if(type != null) {
      this.viewer.stopSounds(type);
    } else if(category != null) {
      this.viewer.stopSounds(category);
    } else {
      this.viewer.stopSounds();
    }
  }

  private static SoundType sponge(final @Nullable Key sound) {
    return sound == null ? null : Adapters.toSponge(SoundType.class, sound);
  }

  private static SoundCategory sponge(final Sound.@Nullable Source source) {
    return source == null ? null : Adapters.toSponge(SoundCategory.class, source, Sound.Source.NAMES);
  }
}
