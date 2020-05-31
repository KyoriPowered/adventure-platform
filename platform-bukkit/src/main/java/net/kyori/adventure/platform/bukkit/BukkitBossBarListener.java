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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/* package */ final class BukkitBossBarListener implements BossBar.Listener {
  private final Map<BossBar, org.bukkit.boss.BossBar> bars = new IdentityHashMap<>();
  private static final BossBar.Flag[] FLAGS = BossBar.Flag.values();

  /* package */ BukkitBossBarListener() {
  }

  @Override
  public void bossBarChanged(final @NonNull BossBar bar, final @NonNull Change type) {
    final org.bukkit.boss.BossBar bukkit = bars.get(bar);
    if(bukkit == null) {
      return;
    }

    if(type == Change.NAME) {
      bukkit.setTitle(legacy(bar.name()));
    } else if(type == Change.PERCENT) {
      bukkit.setProgress(bar.percent());
    } else if(type == Change.COLOR) {
      bukkit.setColor(bukkit(bar.color()));
    } else if(type == Change.OVERLAY) {
      bukkit.setStyle(bukkit(bar.overlay()));
    } else if(type == Change.FLAGS) {
      final Set<BossBar.Flag> flags = bar.flags();
      for(int i = 0, length = FLAGS.length; i < length; i++) {
        final BossBar.Flag flag = FLAGS[i];
        final BarFlag bukkitFlag = bukkit(flag);
        if(flags.contains(flag)) {
          bukkit.addFlag(bukkitFlag);
        } else {
          bukkit.removeFlag(bukkitFlag);
        }
      }
    }
  }

  private static String legacy(final @NonNull Component component) {
    return LegacyComponentSerializer.legacy().serialize(component);
  }

  private static BarColor bukkit(final BossBar.@NonNull Color color) {
    if(color == BossBar.Color.PINK) {
      return BarColor.PINK;
    } else if(color == BossBar.Color.BLUE) {
      return BarColor.BLUE;
    } else if(color == BossBar.Color.RED) {
      return BarColor.RED;
    } else if(color == BossBar.Color.GREEN) {
      return BarColor.GREEN;
    } else if(color == BossBar.Color.YELLOW) {
      return BarColor.YELLOW;
    } else if(color == BossBar.Color.PURPLE) {
      return BarColor.PURPLE;
    } else if(color == BossBar.Color.WHITE) {
      return BarColor.WHITE;
    }
    throw new IllegalArgumentException();
  }

  private static BarFlag bukkit(final BossBar.@NonNull Flag flag) {
    if(flag == BossBar.Flag.DARKEN_SCREEN) {
      return BarFlag.DARKEN_SKY;
    } else if(flag == BossBar.Flag.PLAY_BOSS_MUSIC) {
      return BarFlag.PLAY_BOSS_MUSIC;
    } else if(flag == BossBar.Flag.CREATE_WORLD_FOG) {
      return BarFlag.CREATE_FOG;
    }
    throw new IllegalArgumentException();
  }

  private static BarStyle bukkit(final BossBar.@NonNull Overlay overlay) {
    if(overlay == BossBar.Overlay.PROGRESS) {
      return BarStyle.SOLID;
    } else if(overlay == BossBar.Overlay.NOTCHED_6) {
      return BarStyle.SEGMENTED_6;
    } else if(overlay == BossBar.Overlay.NOTCHED_10) {
      return BarStyle.SEGMENTED_10;
    } else if(overlay == BossBar.Overlay.NOTCHED_12) {
      return BarStyle.SEGMENTED_12;
    } else if(overlay == BossBar.Overlay.NOTCHED_20) {
      return BarStyle.SEGMENTED_20;
    }
    throw new IllegalArgumentException();
  }

  void subscribe(final @NonNull Player player, final @NonNull BossBar bar) {
    final org.bukkit.boss.BossBar bukkit = this.bars.computeIfAbsent(bar, adventure -> {
      org.bukkit.boss.BossBar ret = Bukkit.createBossBar(legacy(adventure.name()), bukkit(adventure.color()), bukkit(adventure.overlay()));
      ret.setProgress(adventure.percent());
      bar.addListener(this);
      return ret;
    });
    bukkit.addPlayer(player);
  }

  void unsubscribe(final @NonNull Player player, final @NonNull BossBar bar) {
    this.bars.computeIfPresent(bar, (adventure, bukkit) -> {
      bukkit.removePlayer(player);
      if(bukkit.getPlayers().isEmpty()) {
        bar.removeListener(this);
        return null;
      } else {
        return bukkit;
      }
    });
  }
}
