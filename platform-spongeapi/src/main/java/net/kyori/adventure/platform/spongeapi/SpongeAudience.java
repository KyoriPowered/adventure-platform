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

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.PlatformAudience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.text.channel.ChatTypeMessageReceiver;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.chat.ChatTypes;

import static java.util.Objects.requireNonNull;

/* package */ class SpongeAudience<M extends MessageReceiver> implements PlatformAudience<M> {
  protected final M receiver;

  public SpongeAudience(M viewer) {
    this.receiver = requireNonNull(viewer, "viewer");
  }

  @Override
  public M viewer() {
    return this.receiver;
  }

  @Override
  public void sendMessage(final @NonNull Component message) {
    this.receiver.sendMessage(SpongePlatform.sponge(message));
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) { }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) { }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    if(this.receiver instanceof ChatTypeMessageReceiver) {
      ((ChatTypeMessageReceiver) this.receiver).sendMessage(ChatTypes.ACTION_BAR, SpongePlatform.sponge(message));
    } else {
      sendMessage(message);
    }
  }

  @Override
  public void playSound(final @NonNull Sound sound) { }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) { }

  @Override
  public void stopSound(final @NonNull SoundStop stop) { }

  @Override
  public void showTitle(final @NonNull Title title) { }

  @Override
  public void clearTitle() { }

  @Override
  public void resetTitle() { }
}
