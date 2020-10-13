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

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

class FacetBossBarListener<V> implements Facet.BossBar<V> {
  private final Facet.BossBar<V> facet;
  private final Supplier<Locale> locale;

  FacetBossBarListener(final Facet.@NonNull BossBar<V> facet, final @NonNull Supplier<Locale> locale) {
    this.facet = facet;
    this.locale = locale;
  }

  @Override
  public void bossBarInitialized(final @NonNull BossBar bar) {
    this.facet.bossBarInitialized(bar);
    this.bossBarNameChanged(bar, bar.name(), bar.name()); // Redo name change with translation
  }

  @Override
  public void bossBarNameChanged(final @NonNull BossBar bar, final @NonNull Component oldName, final @NonNull Component newName) {
    this.facet.bossBarNameChanged(bar, oldName, GlobalTranslator.render(newName, this.locale.get()));
  }

  @Override
  public void bossBarPercentChanged(final @NonNull BossBar bar, final float oldPercent, final float newPercent) {
    this.facet.bossBarPercentChanged(bar, oldPercent, newPercent);
  }

  @Override
  public void bossBarColorChanged(final @NonNull BossBar bar, final BossBar.@NonNull Color oldColor, final BossBar.@NonNull Color newColor) {
    this.facet.bossBarColorChanged(bar, oldColor, newColor);
  }

  @Override
  public void bossBarOverlayChanged(final @NonNull BossBar bar, final BossBar.@NonNull Overlay oldOverlay, final BossBar.@NonNull Overlay newOverlay) {
    this.facet.bossBarOverlayChanged(bar, oldOverlay, newOverlay);
  }

  @Override
  public void bossBarFlagsChanged(final @NonNull BossBar bar, final @NonNull Set<BossBar.Flag> flagsAdded, final @NonNull Set<BossBar.Flag> flagsRemoved) {
    this.facet.bossBarFlagsChanged(bar, flagsAdded, flagsRemoved);
  }

  @Override
  public void addViewer(final @NonNull V viewer) {
    this.facet.addViewer(viewer);
  }

  @Override
  public void removeViewer(final @NonNull V viewer) {
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
