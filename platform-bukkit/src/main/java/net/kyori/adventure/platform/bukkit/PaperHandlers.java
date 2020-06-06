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

import net.kyori.adventure.platform.impl.Handler;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PaperHandlers {

  static class Title implements Handler.Title<Player> {

    @Override
    public boolean isAvailable() {
      return Crafty.hasClass("com.destroystokyo.paper.Title");
    }

    @Override
    public void send(@NonNull final Player viewer, final net.kyori.adventure.title.@NonNull Title title) {
      final com.destroystokyo.paper.Title paperTitle = com.destroystokyo.paper.Title.builder()
        .title(SpigotHandlers.toBungeeCord(title.title()))
        .subtitle(SpigotHandlers.toBungeeCord(title.subtitle()))
        .fadeIn(ticks(title.fadeInTime()))
        .stay(ticks(title.stayTime()))
        .fadeOut(ticks(title.fadeOutTime()))
        .build();
      viewer.sendTitle(paperTitle);
    }

    @Override
    public void clear(@NonNull final Player viewer) {
      viewer.hideTitle();
    }

    @Override
    public void reset(@NonNull final Player viewer) {
      viewer.resetTitle();
    }
  }
}
