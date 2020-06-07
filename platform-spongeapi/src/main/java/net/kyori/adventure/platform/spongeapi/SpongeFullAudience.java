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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.title.Title;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.effect.sound.SoundCategory;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.world.Locatable;

import static java.util.Objects.requireNonNull;

/* package */ final class SpongeFullAudience<V extends MessageReceiver & Viewer> extends SpongeAudience<V> {

  /* package */ SpongeFullAudience(V target) {
    super(target);
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    if(this.viewer() instanceof Player) {
      SpongePlatform.BOSS_BAR_LISTENER.subscribe(requireNonNull(bar, "bar"), (Player) this.viewer());
    }
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    if(this.viewer() instanceof Player) {
      SpongePlatform.BOSS_BAR_LISTENER.unsubscribe(requireNonNull(bar, "bar"), (Player) this.viewer());
    }
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    Vector3d loc = Vector3d.ZERO;
    if(this.viewer() instanceof Locatable) {
      loc = ((Locatable) this.viewer()).getLocation().getPosition();
    }
    playSound(sound, loc);
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    playSound(sound, new Vector3d(x, y, z));
  }

  private void playSound(final @NonNull Sound sound, final @NonNull Vector3d position) {
    requireNonNull(sound, "sound");
    final SoundType type = sponge(sound.name());
    final SoundCategory category = sponge(sound.source());
    this.viewer().playSound(type, category, position, sound.volume(), sound.pitch());
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    requireNonNull(stop, "stop");
    final SoundType type = sponge(stop.sound());
    final SoundCategory category = sponge(stop.source());

    if(type != null && category != null) {
      this.viewer().stopSounds(type, category);
    } else if(type != null) {
      this.viewer().stopSounds(type);
    } else if(category != null) {
      this.viewer().stopSounds(category);
    } else {
      this.viewer().stopSounds();
    }
  }

  @Override
  public void showTitle(final @NonNull Title title) {
    requireNonNull(title, "title");
    this.viewer().sendTitle(org.spongepowered.api.text.title.Title.builder()
      .title(SpongePlatform.sponge(title.title()))
      .subtitle(SpongePlatform.sponge(title.subtitle()))
      .fadeIn(ticks(title.fadeInTime()))
      .fadeOut(ticks(title.fadeOutTime()))
      .stay(ticks(title.stayTime()))
      .build());
  }

  @Override
  public void clearTitle() {
    this.viewer().clearTitle();
  }

  @Override
  public void resetTitle() {
    this.viewer().resetTitle();
  }

  private static int ticks(final @NonNull Duration duration) {
    return duration.getSeconds() == -1 ? -1 : (int) duration.getSeconds() * 20; // TODO: fractions of seconds
  }

  private static SoundType sponge(final @Nullable Key sound) {
    return sound == null ? null : SpongePlatform.sponge(SoundType.class, sound);
  }

  private static SoundCategory sponge(final Sound.@Nullable Source source) {
    return source == null ? null : SpongePlatform.sponge(SoundCategory.class, source, Sound.Source.NAMES);
  }
}
