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

import java.util.Set;
import net.kyori.adventure.bossbar.AbstractBossBar;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.boss.BossBarColor;
import org.spongepowered.api.boss.BossBarOverlay;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.entity.living.player.Player;

class SpongeBossBar extends AbstractBossBar {
  private final ServerBossBar sponge;

  protected SpongeBossBar(@NonNull final Component name, final float percent, @NonNull final Color color, @NonNull final Overlay overlay) {
    super(name, percent, color, overlay);
    this.sponge = ServerBossBar.builder()
      .name(Adapters.toSponge(name))
      .percent(percent)
      .color(Adapters.toSponge(BossBarColor.class, color, Color.NAMES))
      .overlay(Adapters.toSponge(BossBarOverlay.class, overlay, Overlay.NAMES))
      .build();
  }

  @Override
  protected void changed(@NonNull final Change type) {
    if(type == Change.NAME) {
      this.sponge.setName(Adapters.toSponge(this.name()));
    } else if(type == Change.PERCENT) {
      this.sponge.setPercent(this.percent());
    } else if(type == Change.COLOR) {
      this.sponge.setColor(Adapters.toSponge(BossBarColor.class, this.color(), Color.NAMES));
    } else if(type == Change.OVERLAY) {
      this.sponge.setOverlay(Adapters.toSponge(BossBarOverlay.class, this.overlay(), Overlay.NAMES));
    } else if(type == Change.FLAGS) {
      final Set<Flag> flags = this.flags();
      this.sponge.setCreateFog(flags.contains(Flag.CREATE_WORLD_FOG));
      this.sponge.setDarkenSky(flags.contains(Flag.DARKEN_SCREEN));
      this.sponge.setPlayEndBossMusic(flags.contains(Flag.PLAY_BOSS_MUSIC));
    } else {
      // TODO: Warn on unknown change?
    }
  }

  void addPlayer(final Player player) {
    this.sponge.addPlayer(player);
  }

  void removePlayer(final Player player) {
    this.sponge.removePlayer(player);
  }
}
