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
import java.util.function.BiConsumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.SpongeComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.boss.BossBarColor;
import org.spongepowered.api.boss.BossBarOverlay;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.entity.living.player.Player;

/* package */ class SpongeBossBarListener implements BossBar.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(SpongeBossBarListener.class);

  private final Map<BossBar, ServerBossBar> bars = new IdentityHashMap<>();

  SpongeBossBarListener() {
  }

  @Override
  public void bossBarNameChanged(final @NonNull BossBar bar, final @NonNull Component oldName, final @NonNull Component newName) {
    updateBar(bar, newName, (val, sponge) -> sponge.setName(SpongeComponentSerializer.INSTANCE.serialize(val)));
  }

  @Override
  public void bossBarPercentChanged(final @NonNull BossBar bar, final float oldPercent, final float newPercent) {
    updateBar(bar, newPercent, (val, sponge) -> sponge.setPercent(val));
  }

  @Override
  public void bossBarColorChanged(final @NonNull BossBar bar, final BossBar.@NonNull Color oldColor, final BossBar.@NonNull Color newColor) {
    updateBar(bar, newColor, (val, sponge) -> sponge.setColor(SpongePlatform.sponge(BossBarColor.class, val, BossBar.Color.NAMES)));
  }

  @Override
  public void bossBarOverlayChanged(final @NonNull BossBar bar, final BossBar.@NonNull Overlay oldOverlay, final BossBar.@NonNull Overlay newOverlay) {
    updateBar(bar, newOverlay, (val, sponge) -> sponge.setOverlay(SpongePlatform.sponge(BossBarOverlay.class, val, BossBar.Overlay.NAMES)));
  }

  @Override
  public void bossBarFlagsChanged(final @NonNull BossBar bar, final @NonNull Set<BossBar.Flag> oldFlags, final @NonNull Set<BossBar.Flag> newFlags) {
    updateBar(bar, newFlags, (flags, sponge) -> {
      sponge.setCreateFog(flags.contains(BossBar.Flag.CREATE_WORLD_FOG));
      sponge.setDarkenSky(flags.contains(BossBar.Flag.DARKEN_SCREEN));
      sponge.setPlayEndBossMusic(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC));
    });
  }

  private <T> void updateBar(final @NonNull BossBar bar, final @Nullable T change, final @NonNull BiConsumer<T, ServerBossBar> applicator) {
    final ServerBossBar sponge = this.bars.get(bar);
    if(sponge == null) {
      LOGGER.warn("Attached to Adventure BossBar {} but did not have an associated Sponge bar. Ignoring change.", bar);
      return;
    }
    applicator.accept(change, sponge);
  }

  /* package */ void subscribe(final @NonNull BossBar adventure, final @NonNull Player player) {
    this.bars.computeIfAbsent(adventure, key -> {
      key.addListener(this);
      return ServerBossBar.builder()
        .name(SpongeComponentSerializer.INSTANCE.serialize(key.name()))
        .percent(key.percent())
        .color(SpongePlatform.sponge(BossBarColor.class, key.color(), BossBar.Color.NAMES))
        .overlay(SpongePlatform.sponge(BossBarOverlay.class, key.overlay(), BossBar.Overlay.NAMES))
        .createFog(key.flags().contains(BossBar.Flag.CREATE_WORLD_FOG))
        .darkenSky(key.flags().contains(BossBar.Flag.DARKEN_SCREEN))
        .playEndBossMusic(key.flags().contains(BossBar.Flag.PLAY_BOSS_MUSIC))
        .build();
    }).addPlayer(player);
  }

  /* package */ void unsubscribe(final @NonNull BossBar adventure, final @NonNull Player player) {
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
