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
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.effect.sound.SoundCategory;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.channel.ChatTypeMessageReceiver;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.world.Locatable;

class SpongeAudience implements Audience {
  private final MessageReceiver receiver;
  private final Viewer viewer;

  public <T extends MessageReceiver & Viewer> SpongeAudience(T target) {
    this.receiver = target;
    this.viewer = target;
  }

  @Override
  public void message(@NonNull final Component message) {
    receiver.sendMessage(Adapters.toSponge(message));
  }

  @Override
  public void showBossBar(@NonNull final BossBar bar) {
    if(!(bar instanceof SpongeBossBar)) {
      throw new IllegalArgumentException("Submited boss bars must be SPI-created");
    }
    if(viewer instanceof Player) {
      ((SpongeBossBar) bar).sponge().addPlayer((Player) viewer);
    }
  }

  @Override
  public void hideBossBar(@NonNull final BossBar bar) {
    if(!(bar instanceof SpongeBossBar)) {
      throw new IllegalArgumentException("Submited boss bars must be SPI-created");
    }
    if(viewer instanceof Player) {
      ((SpongeBossBar) bar).sponge().removePlayer((Player) viewer);
    }
  }

  @Override
  public void showActionBar(@NonNull final Component message) {
    if(this.receiver instanceof ChatTypeMessageReceiver) {
      ((ChatTypeMessageReceiver) this.receiver).sendMessage(ChatTypes.ACTION_BAR, Adapters.toSponge(message));
    }
  }

  @Override
  public void playSound(@NonNull final Sound sound) {
    Vector3d loc = Vector3d.ZERO;
    if(this.viewer instanceof Locatable) {
      loc = ((Locatable) this.viewer).getLocation().getPosition();
    }
    final SoundType type = Adapters.toSponge(SoundType.class, sound.name());
    final SoundCategory category = Adapters.toSponge(SoundCategory.class, sound.source(), Sound.Source.NAMES);
    this.viewer.playSound(type, category, loc, sound.volume(), sound.pitch());
  }

  @Override
  public void stopSound(@NonNull final SoundStop stop) {
    final SoundType type = stop.sound() == null ? null : Adapters.toSponge(SoundType.class, stop.sound());
    final SoundCategory category = stop.source() == null ? null : Adapters.toSponge(SoundCategory.class, stop.source(), Sound.Source.NAMES);

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
}
