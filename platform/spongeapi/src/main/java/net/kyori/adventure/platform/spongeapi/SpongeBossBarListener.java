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
package net.kyori.adventure.platform.spongeapi;

import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.common.AbstractBossBarListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.spongeapi.SpongeApiComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.boss.BossBarColor;
import org.spongepowered.api.boss.BossBarOverlay;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.entity.living.player.Player;

/* package */ class SpongeBossBarListener extends AbstractBossBarListener<Player, ServerBossBar> {

  SpongeBossBarListener() {
  }

  @Override
  public void bossBarNameChanged(final @NonNull BossBar bar, final @NonNull Component oldName, final @NonNull Component newName) {
    handle(bar, newName, (val, sponge) -> sponge.setName(SpongeApiComponentSerializer.get().serialize(val)));
  }

  @Override
  public void bossBarPercentChanged(final @NonNull BossBar bar, final float oldPercent, final float newPercent) {
    handle(bar, newPercent, (val, sponge) -> sponge.setPercent(val));
  }

  @Override
  public void bossBarColorChanged(final @NonNull BossBar bar, final BossBar.@NonNull Color oldColor, final BossBar.@NonNull Color newColor) {
    handle(bar, newColor, (val, sponge) -> sponge.setColor(SpongeAudienceProvider.sponge(BossBarColor.class, val, BossBar.Color.NAMES)));
  }

  @Override
  public void bossBarOverlayChanged(final @NonNull BossBar bar, final BossBar.@NonNull Overlay oldOverlay, final BossBar.@NonNull Overlay newOverlay) {
    handle(bar, newOverlay, (val, sponge) -> sponge.setOverlay(SpongeAudienceProvider.sponge(BossBarOverlay.class, val, BossBar.Overlay.NAMES)));
  }

  @Override
  public void bossBarFlagsChanged(final @NonNull BossBar bar, final @NonNull Set<BossBar.Flag> oldFlags, final @NonNull Set<BossBar.Flag> newFlags) {
    handle(bar, newFlags, (flags, sponge) -> {
      sponge.setCreateFog(flags.contains(BossBar.Flag.CREATE_WORLD_FOG));
      sponge.setDarkenSky(flags.contains(BossBar.Flag.DARKEN_SCREEN));
      sponge.setPlayEndBossMusic(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC));
    });
  }

  @NonNull
  @Override
  protected ServerBossBar newInstance(final @NonNull BossBar adventure) {
    return ServerBossBar.builder()
      .name(SpongeApiComponentSerializer.get().serialize(adventure.name()))
      .percent(adventure.percent())
      .color(SpongeAudienceProvider.sponge(BossBarColor.class, adventure.color(), BossBar.Color.NAMES))
      .overlay(SpongeAudienceProvider.sponge(BossBarOverlay.class, adventure.overlay(), BossBar.Overlay.NAMES))
      .createFog(adventure.flags().contains(BossBar.Flag.CREATE_WORLD_FOG))
      .darkenSky(adventure.flags().contains(BossBar.Flag.DARKEN_SCREEN))
      .playEndBossMusic(adventure.flags().contains(BossBar.Flag.PLAY_BOSS_MUSIC))
      .build();
  }

  @Override
  protected void show(final @NonNull Player viewer, final @NonNull ServerBossBar bar) {
    bar.addPlayer(viewer);
  }

  @Override
  protected boolean hide(final @NonNull Player viewer, final @NonNull ServerBossBar bar) {
    final boolean had = bar.getPlayers().contains(viewer);
    bar.removePlayer(viewer);
    return had;
  }

  @Override
  protected boolean isEmpty(final @NonNull ServerBossBar bar) {
    return bar.getPlayers().isEmpty();
  }

  @Override
  protected void hideFromAll(final @NonNull ServerBossBar bar) {
    bar.removePlayers(bar.getPlayers());
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}
