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
package net.kyori.adventure.platform.common;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import net.kyori.adventure.bossbar.BossBar;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractBossBarListener<V, I> implements Handler.BossBars<V>, BossBar.Listener {
  private final Map<BossBar, I> bars = Collections.synchronizedMap(new IdentityHashMap<>());

  @Override
  public void show(@NonNull final V viewer, final @NonNull BossBar bar) {
    final I instance = this.bars.computeIfAbsent(bar, adventure -> {
      adventure.addListener(this);
      return this.newInstance(adventure);
    });
    this.show(viewer, instance);
  }

  protected <T> void handle(final BossBar adventure, final T changedValue, final BiConsumer<T, I> handler) {
    final I instance = this.bars.get(adventure);
    if(instance != null) {
      handler.accept(changedValue, instance);
    }
  }

  protected abstract @NonNull I newInstance(final @NonNull BossBar adventure);

  protected abstract void show(final @NonNull V viewer, final @NonNull I bar);

  protected abstract boolean hide(final @NonNull V viewer, final @NonNull I bar);

  protected abstract boolean isEmpty(final @NonNull I bar);

  protected abstract void hideFromAll(final @NonNull I bar);

  @Override
  public void hide(@NonNull final V viewer, final @NonNull BossBar bar) {
    this.bars.computeIfPresent(bar, (adventure, existing) -> {
      this.hide(viewer, existing);
      if(this.isEmpty(existing)) {
        return null;
      } else {
        return existing;
      }
    });
  }

  @Override
  public void hideAll(@NonNull final V viewer) {
    for(final Iterator<Map.Entry<BossBar, I>> it = this.bars.entrySet().iterator(); it.hasNext();) {
      final Map.Entry<BossBar, I> entry = it.next();
      if(this.hide(viewer, entry.getValue())) {
        if(this.isEmpty(entry.getValue())) {
          entry.getKey().removeListener(this);
          it.remove();
        }
      }
    }
  }

  @Override
  public void hideAll() {
    for(final Map.Entry<BossBar, I> entry : this.bars.entrySet()) {
      entry.getKey().removeListener(this);
      this.hideFromAll(entry.getValue());
    }
    this.bars.clear();
  }

}
