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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/* package */ final class BukkitBossBarListener implements BossBar.Listener {
  private static final BossBar.Flag[] FLAGS = BossBar.Flag.values();
  private static final HandlerCollection<org.bukkit.boss.BossBar, NameSetter> SET_NAME = new HandlerCollection<>(new CraftBukkitHandlers.BossBarNameSetter(), new BukkitHandlers.BossBarNameSetter());

  private final Map<BossBar, org.bukkit.boss.BossBar> bars = new IdentityHashMap<>();

  /* package */ BukkitBossBarListener() {
  }

  private void withBar(final BossBar bar, final Consumer<org.bukkit.boss.BossBar> consumer) {
    final org.bukkit.boss.BossBar bukkit = this.bars.get(bar);
    if(bukkit != null) {
      consumer.accept(bukkit);
    }
  }

  @Override
  public void bossBarNameChanged(@NonNull final BossBar bar, @NonNull final Component oldName, @NonNull final Component newName) {
    this.withBar(bar, bukkit -> {
      final NameSetter setter = SET_NAME.get(bukkit);
      if(setter != null) {
        setter.setName(bukkit, bar.name());
      }
    });
  }

  @Override
  public void bossBarPercentChanged(@NonNull final BossBar bar, final float oldPercent, final float newPercent) {
    this.withBar(bar, bukkit -> bukkit.setProgress(newPercent));
  }

  @Override
  public void bossBarColorChanged(@NonNull final BossBar bar, final BossBar.@NonNull Color oldColor, final BossBar.@NonNull Color newColor) {
    this.withBar(bar, bukkit -> bukkit.setColor(bukkit(newColor)));
  }

  @Override
  public void bossBarOverlayChanged(@NonNull final BossBar bar, final BossBar.@NonNull Overlay oldOverlay, final BossBar.@NonNull Overlay newOverlay) {
    this.withBar(bar, bukkit -> bukkit.setStyle(bukkit(newOverlay)));
  }

  @Override
  public void bossBarFlagsChanged(@NonNull final BossBar bar, @NonNull final Set<BossBar.Flag> oldFlags, @NonNull final Set<BossBar.Flag> newFlags) {
    this.withBar(bar, bukkit -> {
      for(int i = 0, length = FLAGS.length; i < length; i++) {
        final BossBar.Flag flag = FLAGS[i];
        final BarFlag bukkitFlag = bukkit(flag);
        if(newFlags.contains(flag)) {
          bukkit.addFlag(bukkitFlag);
        } else {
          bukkit.removeFlag(bukkitFlag);
        }
      }
    });
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
      final org.bukkit.boss.BossBar ret = Bukkit.createBossBar("", bukkit(adventure.color()), bukkit(adventure.overlay()));
      final NameSetter nameSetter = SET_NAME.get(ret);
      if(nameSetter != null) {
        nameSetter.setName(ret, adventure.name());
      }
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

  public void unsubscribeFromAll(final @NonNull Player player) {
    for(Iterator<org.bukkit.boss.BossBar> it = this.bars.values().iterator(); it.hasNext();) {
      final org.bukkit.boss.BossBar bukkit = it.next();
      bukkit.removePlayer(player);
      if(bukkit.getPlayers().isEmpty()) {
        it.remove();
      }
    }
  }

  /**
   * Set the name on a Bukkit boss bar.
   */
  interface NameSetter extends Handler<org.bukkit.boss.BossBar> {
    void setName(org.bukkit.boss.@NonNull BossBar bar, @NonNull Component name);
  }
}
