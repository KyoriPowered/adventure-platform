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
import java.util.UUID;
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
   * Check if this handler can process for a specific viewer.
   *
   * <p>This check will only be performed if {@link #isAvailable()} has already returned true.</p>
   * 
   * @param viewer viewer to check
   * @return availability for this viewer
   */
  default boolean isAvailable(final @NonNull V viewer) {
    return true;
  }

  interface Chat<V, S> extends Handler<V> {
    UUID NIL_UUID = new UUID(0, 0);

    S initState(final @NonNull Component component);
    
    void send(final @NonNull V target, final @NonNull S message);
  }

  interface ActionBar<V, S> extends Handler<V> {
    S initState(final @NonNull Component message);
    
    void send(final @NonNull V viewer, final @NonNull S message);
  }

  interface Title<V> extends Handler<V> {
    /**
     * Instruct the client to keep its current value for a duration field on the title
     */
    int DURATION_PRESERVE = -1;

    void send(final @NonNull V viewer, final net.kyori.adventure.title.@NonNull Title title);

    void clear(final @NonNull V viewer);

    void reset(final @NonNull V viewer);
    
    static int ticks(final @NonNull Duration time) {
      return time.isNegative() ? DURATION_PRESERVE : (int) Math.ceil(time.toMillis() / 50.0);
    }
  }

  interface BossBar<V> extends Handler<V> {
    void show(final @NonNull V viewer, final net.kyori.adventure.bossbar.@NonNull BossBar bar);
    void hide(final @NonNull V viewer, final net.kyori.adventure.bossbar.@NonNull BossBar bar);
  }
  
  interface PlaySound<V> extends Handler<V> {
    void play(final @NonNull V viewer, final @NonNull Sound sound);
    
    void play(final @NonNull V viewer, final @NonNull Sound sound, final double x, final double y, final double z);
    
    void stop(final @NonNull V viewer, final @NonNull SoundStop sound);
  }
}
