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
package net.kyori.adventure.platform.impl;

import java.time.Duration;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A handler for a unit of functionality that may differ between platforms.
 * 
 * @param <V> Native viewer type
 */
public interface Handler<V> {
  /**
   * Will this handler ever be functional in the current runtime environment?
   * 
   * <p>If not, this handler will not be checked when sending to audiences.
   * 
   * @return availability
   */
  boolean isAvailable();

  /**
   * Check if this handler can process for a specific viewer
   * 
   * @param viewer viewer to check
   * @return availability for this viewer
   */
  default boolean isAvailable(V viewer) {
    return isAvailable();
  }

  interface Chat<V, S> extends Handler<V> {
    S initState(Component component);
    
    void send(@NonNull V target, @NonNull S message);
  }

  interface ActionBar<V, S> extends Handler<V> {
    S initState(Component message);
    
    void send(@NonNull V viewer, @NonNull S message);
  }

  interface Title<V> extends Handler<V> {
    void send(@NonNull V viewer, net.kyori.adventure.title.@NonNull Title title);

    void clear(@NonNull V viewer);

    void reset(@NonNull V viewer);
    
    default int ticks(Duration time) {
      final int seconds = (int) time.getSeconds();
      return seconds == -1 ? -1 : 20 * seconds;
    }
  }

  interface BossBar<V> extends Handler<V> {
    void show(@NonNull V viewer, net.kyori.adventure.bossbar.@NonNull BossBar bar);
    void hide(@NonNull V viewer, net.kyori.adventure.bossbar.@NonNull BossBar bar);
  }
  
  interface PlaySound<V> extends Handler<V> {
    void play(@NonNull V viewer, @NonNull Sound sound);
    
    void play(@NonNull V viewer, @NonNull Sound sound, double x, double y, double z);
    
    void stop(@NonNull V viewer, @NonNull SoundStop sound);
  }

}
