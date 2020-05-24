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

import net.kyori.adventure.bossbar.AbstractBossBar;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.boss.BossBarColor;
import org.spongepowered.api.boss.BossBarOverlay;
import org.spongepowered.api.boss.ServerBossBar;

class SpongeBossBar extends AbstractBossBar {
  private final ServerBossBar spongeBar;

  protected SpongeBossBar(@NonNull final Component name, final float percent, @NonNull final Color color, @NonNull final Overlay overlay) {
    super(name, percent, color, overlay);
    this.spongeBar = ServerBossBar.builder()
      .name(Adapters.toSponge(name))
      .percent(percent)
      .color(Adapters.toSponge(BossBarColor.class, color, Color.NAMES))
      .overlay(Adapters.toSponge(BossBarOverlay.class, overlay, Overlay.NAMES))
      .build();
  }

  @Override
  protected void changed(@NonNull final Change type) {
    switch(type) {
      case NAME:
        this.sponge().setName(Adapters.toSponge(this.name()));
        break;
      case PERCENT:
        this.sponge().setPercent(this.percent());
        break;
      case COLOR:
        this.sponge().setColor(Adapters.toSponge(BossBarColor.class, this.color(), Color.NAMES));
        break;
      case OVERLAY:
        this.sponge().setOverlay(Adapters.toSponge(BossBarOverlay.class, this.overlay(), Overlay.NAMES));
        break;
      case FLAGS:
        this.sponge().setCreateFog(flags().contains(Flag.CREATE_WORLD_FOG));
        this.sponge().setDarkenSky(flags().contains(Flag.DARKEN_SCREEN));
        this.sponge().setPlayEndBossMusic(flags().contains(Flag.PLAY_BOSS_MUSIC));
        break;
      default: // TODO: Warn on unknown change?
    }
  }

  public ServerBossBar sponge() {
    return this.spongeBar;
  }
}
