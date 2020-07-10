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
package net.kyori.adventure.platform.bukkit;

import com.destroystokyo.paper.Title;
import java.time.Duration;
import java.util.function.IntConsumer;
import net.kyori.adventure.platform.common.Handler;
import net.kyori.adventure.platform.common.Knobs;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/* package */ final class PaperHandlers {
  
  private static final boolean ENABLED = Knobs.enabled("paper");
  
  private PaperHandlers() {
  }

  /* package */ static class Titles implements Handler.Titles<Player> {

    @Override
    public boolean isAvailable() {
      return SpigotHandlers.BOUND && ENABLED && Crafty.hasClass("com.destroystokyo.paper.Title");
    }

    @Override
    public void send(final @NonNull Player viewer, final net.kyori.adventure.title.@NonNull Title title) {
      final Title.Builder paperTitle = Title.builder()
        .title(SpigotHandlers.SERIALIZER.serialize(title.title()))
        .subtitle(SpigotHandlers.SERIALIZER.serialize(title.subtitle()));

      applyTime(title.fadeInTime(), paperTitle::fadeIn);
      applyTime(title.stayTime(), paperTitle::stay);
      applyTime(title.fadeOutTime(), paperTitle::fadeOut);

      viewer.sendTitle(paperTitle.build());
    }

    /**
     * Paper will reject negative durations, so we have to resort to every send assigning default times.
     * @param time The time to send
     * @param consumer Time builder
     */
    private static void applyTime(final Duration time, final IntConsumer consumer) {
      final int ticks = Titles.ticks(time);
      if(ticks != Titles.DURATION_PRESERVE) {
        consumer.accept(ticks);
      }
    }

    @Override
    public void clear(final @NonNull Player viewer) {
      viewer.hideTitle();
    }

    @Override
    public void reset(final @NonNull Player viewer) {
      viewer.resetTitle();
    }
  }
}
