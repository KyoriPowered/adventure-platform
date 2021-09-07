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
package net.kyori.adventure.platform.facet;

import java.util.Set;
import java.util.function.Function;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

class FacetBossBarListener<V> implements Facet.BossBar<V> {
  private final Facet.BossBar<V> facet;
  private final Function<Component, Component> translator;

  FacetBossBarListener(final Facet.@NotNull BossBar<V> facet, final @NotNull Function<Component, Component> translator) {
    this.facet = facet;
    this.translator = translator;
  }

  @Override
  public void bossBarInitialized(final @NotNull BossBar bar) {
    this.facet.bossBarInitialized(bar);
    this.bossBarNameChanged(bar, bar.name(), bar.name()); // Redo name change with translation
  }

  @Override
  public void bossBarNameChanged(final @NotNull BossBar bar, final @NotNull Component oldName, final @NotNull Component newName) {
    this.facet.bossBarNameChanged(bar, oldName, this.translator.apply(newName));
  }

  @Override
  public void bossBarProgressChanged(final @NotNull BossBar bar, final float oldPercent, final float newPercent) {
    this.facet.bossBarProgressChanged(bar, oldPercent, newPercent);
  }

  @Override
  public void bossBarColorChanged(final @NotNull BossBar bar, final BossBar.@NotNull Color oldColor, final BossBar.@NotNull Color newColor) {
    this.facet.bossBarColorChanged(bar, oldColor, newColor);
  }

  @Override
  public void bossBarOverlayChanged(final @NotNull BossBar bar, final BossBar.@NotNull Overlay oldOverlay, final BossBar.@NotNull Overlay newOverlay) {
    this.facet.bossBarOverlayChanged(bar, oldOverlay, newOverlay);
  }

  @Override
  public void bossBarFlagsChanged(final @NotNull BossBar bar, final @NotNull Set<BossBar.Flag> flagsAdded, final @NotNull Set<BossBar.Flag> flagsRemoved) {
    this.facet.bossBarFlagsChanged(bar, flagsAdded, flagsRemoved);
  }

  @Override
  public void addViewer(final @NotNull V viewer) {
    this.facet.addViewer(viewer);
  }

  @Override
  public void removeViewer(final @NotNull V viewer) {
    this.facet.removeViewer(viewer);
  }

  @Override
  public boolean isEmpty() {
    return this.facet.isEmpty();
  }

  @Override
  public void close() {
    this.facet.close();
  }
}
