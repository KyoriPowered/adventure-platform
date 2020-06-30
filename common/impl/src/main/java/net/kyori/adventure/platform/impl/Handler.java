/*
 * This file is part of adventure-platform, licensed under the MIT License.
 *
 * Copyright (c) 2018-2020 KyoriPowered
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
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
    byte TYPE_CHAT = 0;
    byte TYPE_SYSTEM = 1;
    byte TYPE_ACTIONBAR = 2;

    S initState(final @NonNull Component component);
    
    void send(final @NonNull V target, final @NonNull S message);
  }

  interface ActionBar<V, S> extends Handler<V> {
    S initState(final @NonNull Component message);
    
    void send(final @NonNull V viewer, final @NonNull S message);
  }

  interface Titles<V> extends Handler<V> {
    /**
     * Instruct the client to keep its current value for a duration field on the title
     */
    int DURATION_PRESERVE = -1;

    void showTitle(final @NonNull V viewer, final @NonNull Component title, final @NonNull Component subtitle, final int inTicks, final int stayTicks, final int outTicks);

    void clearTitle(final @NonNull V viewer);

    void resetTitle(final @NonNull V viewer);
    
    static int ticks(final @NonNull Duration time) {
      return time.isNegative() ? DURATION_PRESERVE : (int) Math.ceil(time.toMillis() / 50.0);
    }
  }

  interface BossBars<V> extends Handler<V> {
    // packet actions
    int ACTION_ADD = 0; // (name: String, percent: float, color: varint, overlay: varint, flags: ubyte)
    int ACTION_REMOVE = 1; // ()
    int ACTION_PERCENT = 2; // (float)
    int ACTION_NAME = 3; // (String)
    int ACTION_STYLE = 4; // (color: varint, overlay: varint)
    int ACTION_FLAGS = 5; // (thickenFog | dragonMusic | darkenSky): ubyte

    // flags
    byte FLAG_DARKEN_SCREEN = 1;
    byte FLAG_BOSS_MUSIC = 1 << 1;
    byte FLAG_CREATE_WORLD_FOG = 1 << 2;

    static byte bitmaskFlags(final @NonNull Set<BossBar.Flag> flags) {
      byte ret = 0;
      if(flags.contains(BossBar.Flag.DARKEN_SCREEN)) {
        ret |= FLAG_DARKEN_SCREEN;
      }
      if(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC)) {
        ret |= FLAG_BOSS_MUSIC;
      }
      if(flags.contains(BossBar.Flag.CREATE_WORLD_FOG)) {
        ret |= FLAG_CREATE_WORLD_FOG;
      }
      return ret;
    }

    static int color(final BossBar.Color color) {
      switch(color) {
        case PINK: return 0;
        case BLUE: return 1;
        case RED: return 2;
        case GREEN: return 3;
        case YELLOW: return 4;
        case WHITE: return 6;
        case PURPLE: /* fall-through, out of order */
        default: return 5;
      }
    }

    static int overlay(final BossBar.Overlay overlay) {
      switch(overlay) {
        case NOTCHED_6: return 1;
        case NOTCHED_10: return 2;
        case NOTCHED_12: return 3;
        case NOTCHED_20: return 4;
        case PROGRESS: /* fall-through */
        default: return 0;
      }
    }

    void show(final @NonNull V viewer, final @NonNull BossBar bar);
    void hide(final @NonNull V viewer, final @NonNull BossBar bar);

    /**
     * Remove the viewer from all handled boss bars
     *
     * @param viewer viewer to hide all boss bars for.
     */
    void hideAll(final @NonNull V viewer);

    /**
     * Hide every boss bar from every associated viewer
     */
    void hideAll();
  }
  
  interface PlaySound<V> extends Handler<V> {
    void play(final @NonNull V viewer, final @NonNull Sound sound);
    
    void play(final @NonNull V viewer, final @NonNull Sound sound, final double x, final double y, final double z);
    
    void stop(final @NonNull V viewer, final @NonNull SoundStop sound);
  }

  interface Books<V> extends Handler<V> {
    void openBook(final @NonNull V viewer, final @NonNull Component title, final @NonNull Component author, final @NonNull Iterable<Component> pages);
  }
}
