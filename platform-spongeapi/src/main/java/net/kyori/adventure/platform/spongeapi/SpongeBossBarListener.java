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
import net.kyori.adventure.bossbar.BossBar;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.boss.BossBarColor;
import org.spongepowered.api.boss.BossBarOverlay;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.entity.living.player.Player;

class SpongeBossBarListener implements BossBar.Listener {
  private final ServerBossBar sponge;

  protected SpongeBossBarListener(BossBar bar) {
    this.sponge = ServerBossBar.builder()
      .name(Adapters.toSponge(bar.name()))
      .percent(bar.percent())
      .color(Adapters.toSponge(BossBarColor.class, bar.color(), BossBar.Color.NAMES))
      .overlay(Adapters.toSponge(BossBarOverlay.class, bar.overlay(), BossBar.Overlay.NAMES))
      .build();
  }

  @Override
  public void bossBarChanged(final @NonNull BossBar bar, final @NonNull Change type) {
    if(type == Change.NAME) {
      this.sponge.setName(Adapters.toSponge(bar.name()));
    } else if(type == Change.PERCENT) {
      this.sponge.setPercent(bar.percent());
    } else if(type == Change.COLOR) {
      this.sponge.setColor(Adapters.toSponge(BossBarColor.class, bar.color(), BossBar.Color.NAMES));
    } else if(type == Change.OVERLAY) {
      this.sponge.setOverlay(Adapters.toSponge(BossBarOverlay.class, bar.overlay(), BossBar.Overlay.NAMES));
    } else if(type == Change.FLAGS) {
      final Set<BossBar.Flag> flags = bar.flags();
      this.sponge.setCreateFog(flags.contains(BossBar.Flag.CREATE_WORLD_FOG));
      this.sponge.setDarkenSky(flags.contains(BossBar.Flag.DARKEN_SCREEN));
      this.sponge.setPlayEndBossMusic(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC));
    } else {
      // TODO: Warn on unknown change?
    }
  }

  void subscribe(final Player player) {
    this.sponge.addPlayer(player);
  }

  void unsubscribe(final Player player) {
    this.sponge.removePlayer(player);
  }
}
