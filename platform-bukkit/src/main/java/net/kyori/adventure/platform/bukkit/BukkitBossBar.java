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

import java.util.Set;
import net.kyori.adventure.bossbar.AbstractBossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

final class BukkitBossBar extends AbstractBossBar {
  private static final Flag[] FLAGS = Flag.values();
  private final BossBar bukkit;

  BukkitBossBar(final @NonNull Component name, final float percent, final @NonNull Color color, final @NonNull Overlay overlay) {
    super(name, percent, color, overlay);
    this.bukkit = Bukkit.getServer().createBossBar(legacy(name), bukkit(color), bukkit(overlay));
  }

  @Override
  protected void changed(final @NonNull Change type) {
    if(type == Change.NAME) {
      this.bukkit.setTitle(legacy(this.name()));
    } else if(type == Change.PERCENT) {
      this.bukkit.setProgress(this.percent());
    } else if(type == Change.COLOR) {
      this.bukkit.setColor(bukkit(this.color()));
    } else if(type == Change.OVERLAY) {
      this.bukkit.setStyle(bukkit(this.overlay()));
    } else if(type == Change.FLAGS) {
      final Set<Flag> flags = this.flags();
      for(int i = 0, length = FLAGS.length; i < length; i++) {
        final Flag flag = FLAGS[i];
        final BarFlag bukkit = bukkit(flag);
        if(flags.contains(flag)) {
          this.bukkit.addFlag(bukkit);
        } else {
          this.bukkit.removeFlag(bukkit);
        }
      }
    }
  }

  private static String legacy(final Component component) {
    return LegacyComponentSerializer.legacy().serialize(component);
  }

  private static BarColor bukkit(final Color color) {
    if(color == Color.PINK) {
      return BarColor.PINK;
    } else if(color == Color.BLUE) {
      return BarColor.BLUE;
    } else if(color == Color.RED) {
      return BarColor.RED;
    } else if(color == Color.GREEN) {
      return BarColor.GREEN;
    } else if(color == Color.YELLOW) {
      return BarColor.YELLOW;
    } else if(color == Color.PURPLE) {
      return BarColor.PURPLE;
    } else if(color == Color.WHITE) {
      return BarColor.WHITE;
    }
    throw new IllegalArgumentException();
  }

  private static BarFlag bukkit(final Flag flag) {
    if(flag == Flag.DARKEN_SCREEN) {
      return BarFlag.DARKEN_SKY;
    } else if(flag == Flag.PLAY_BOSS_MUSIC) {
      return BarFlag.PLAY_BOSS_MUSIC;
    } else if(flag == Flag.CREATE_WORLD_FOG) {
      return BarFlag.CREATE_FOG;
    }
    throw new IllegalArgumentException();
  }

  private static BarStyle bukkit(final Overlay overlay) {
    if(overlay == Overlay.PROGRESS) {
      return BarStyle.SOLID;
    } else if(overlay == Overlay.NOTCHED_6) {
      return BarStyle.SEGMENTED_6;
    } else if(overlay == Overlay.NOTCHED_10) {
      return BarStyle.SEGMENTED_10;
    } else if(overlay == Overlay.NOTCHED_12) {
      return BarStyle.SEGMENTED_12;
    } else if(overlay == Overlay.NOTCHED_20) {
      return BarStyle.SEGMENTED_20;
    }
    throw new IllegalArgumentException();
  }

  void addPlayer(final Player player) {
    this.bukkit.addPlayer(player);
  }

  void removePlayer(final Player player) {
    this.bukkit.removePlayer(player);
  }
}
