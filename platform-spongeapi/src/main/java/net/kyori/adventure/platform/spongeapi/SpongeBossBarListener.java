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

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.boss.BossBarColor;
import org.spongepowered.api.boss.BossBarOverlay;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.entity.living.player.Player;

class SpongeBossBarListener implements BossBar.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(SpongeBossBarListener.class);

  private final Map<BossBar, ServerBossBar> bars = new IdentityHashMap<>();

  SpongeBossBarListener() {
  }

  @Override
  public void bossBarChanged(final @NonNull BossBar bar, final @NonNull Change type) {
    final ServerBossBar sponge = this.bars.get(bar);
    if(sponge == null) {
      LOGGER.warn("Attached to Adventure BossBar {} but did not have an associated Sponge bar. Ignoring change.", bar);
      return;
    }
    if(type == Change.NAME) {
      sponge.setName(SpongePlatform.sponge(bar.name()));
    } else if(type == Change.PERCENT) {
      sponge.setPercent(bar.percent());
    } else if(type == Change.COLOR) {
      sponge.setColor(SpongePlatform.sponge(BossBarColor.class, bar.color(), BossBar.Color.NAMES));
    } else if(type == Change.OVERLAY) {
      sponge.setOverlay(SpongePlatform.sponge(BossBarOverlay.class, bar.overlay(), BossBar.Overlay.NAMES));
    } else if(type == Change.FLAGS) {
      final Set<BossBar.Flag> flags = bar.flags();
      sponge.setCreateFog(flags.contains(BossBar.Flag.CREATE_WORLD_FOG));
      sponge.setDarkenSky(flags.contains(BossBar.Flag.DARKEN_SCREEN));
      sponge.setPlayEndBossMusic(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC));
    } else {
      // TODO: Warn on unknown change?
    }
  }

  void subscribe(final @NonNull BossBar adventure, final @NonNull Player player) {
    this.bars.computeIfAbsent(adventure, key -> {
      adventure.addListener(this);
      return ServerBossBar.builder()
        .name(SpongePlatform.sponge(key.name()))
        .percent(key.percent())
        .color(SpongePlatform.sponge(BossBarColor.class, key.color(), BossBar.Color.NAMES))
        .overlay(SpongePlatform.sponge(BossBarOverlay.class, key.overlay(), BossBar.Overlay.NAMES))
        .createFog(key.flags().contains(BossBar.Flag.CREATE_WORLD_FOG))
        .darkenSky(key.flags().contains(BossBar.Flag.DARKEN_SCREEN))
        .playEndBossMusic(key.flags().contains(BossBar.Flag.PLAY_BOSS_MUSIC))
        .build();
    }).addPlayer(player);
  }

  void unsubscribe(final @NonNull BossBar adventure, final @NonNull Player player) {
    this.bars.computeIfPresent(adventure, (key, existing) -> {
      existing.removePlayer(player);
      if(existing.getPlayers().isEmpty()) {
        key.removeListener(this);
        return null;
      } else {
        return existing;
      }
    });
  }
}
