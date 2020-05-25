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
import org.bukkit.command.ConsoleCommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ConsoleAudience implements Audience {
  private final ConsoleCommandSender viewer;

  public ConsoleAudience(final @NonNull ConsoleCommandSender viewer) {
    this.viewer = viewer;
  }

  @Override
  public void message(final @NonNull Component message) {
    TextAdapter.sendMessage(this.viewer, message);
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    // NOOP
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    // NOOP
  }

  @Override
  public void showActionBar(final @NonNull Component message) {
    // NOOP
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    // NOOP
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    // NOOP
  }
}
