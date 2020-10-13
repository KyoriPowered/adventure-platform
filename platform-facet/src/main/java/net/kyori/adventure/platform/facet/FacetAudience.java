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

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.util.Objects.requireNonNull;

/**
 * An {@link Audience} that is implemented by {@link Facet}s.
 *
 * <p>This audience must support multiple viewers, although platforms do not use this feature yet.</p>
 *
 * @param <V> a viewer type
 * @see Facet
 */
public class FacetAudience<V> implements Audience, Closeable {
  private final @NonNull Set<V> viewers;
  private volatile @Nullable V viewer; // The first viewer is used for facet and message selection
  private volatile @NonNull Locale locale;

  private final Facet.@Nullable Chat<V, Object> chat;
  private final Facet.@Nullable ActionBar<V, Object> actionBar;
  private final Facet.@Nullable Title<V, Object, Object> title;
  private final Facet.@Nullable Sound<V, Object> sound;
  private final Facet.@Nullable Book<V, Object, Object> book;
  private final Facet.BossBar.@Nullable Builder<V, Facet.BossBar<V>> bossBar;
  private final @Nullable Map<BossBar, Facet.BossBar<V>> bossBars;

  @SuppressWarnings({"unchecked", "rawtypes"}) // Without suppression, this constructor becomes unreadable
  public FacetAudience(
    final @NonNull Collection<? extends V> viewers,
    final @Nullable Locale locale,
    final @Nullable Collection<? extends Facet.Chat> chat,
    final @Nullable Collection<? extends Facet.ActionBar> actionBar,
    final @Nullable Collection<? extends Facet.Title> title,
    final @Nullable Collection<? extends Facet.Sound> sound,
    final @Nullable Collection<? extends Facet.Book> book,
    final @Nullable Collection<? extends Facet.BossBar.Builder> bossBar
  ) {
    this.viewers = new CopyOnWriteArraySet<>();
    this.locale = locale == null ? Locale.US : locale;
    for(final V viewer : requireNonNull(viewers, "viewers")) {
      this.addViewer(viewer);
    }
    this.chat = Facet.of(chat, this.viewer);
    this.actionBar = Facet.of(actionBar, this.viewer);
    this.title = Facet.of(title, this.viewer);
    this.sound = Facet.of(sound, this.viewer);
    this.book = Facet.of(book, this.viewer);
    this.bossBar = Facet.of(bossBar, this.viewer);
    this.bossBars = this.bossBar == null ? null : Collections.synchronizedMap(new IdentityHashMap<>(4));
  }

  public void addViewer(final @NonNull V viewer) {
    if(this.viewers.add(viewer) && this.viewer == null) {
      this.viewer = viewer;
    }
  }

  public void removeViewer(final @NonNull V viewer) {
    if(this.viewers.remove(viewer) && this.viewer == viewer) {
      this.viewer = this.viewers.isEmpty() ? null : this.viewers.iterator().next();
    }

    if(this.bossBars == null) return;
    for(final Facet.BossBar<V> listener : this.bossBars.values()) {
      listener.removeViewer(viewer);
    }
  }

  /**
   * Changes the locale.
   *
   * @param locale a locale
   */
  public void changeLocale(final @NonNull Locale locale) {
    this.locale = requireNonNull(locale, "locale");
  }

  @Override
  public void sendMessage(final @NonNull Component original, final @NonNull MessageType type) {
    if(this.chat == null) return;

    final Object message = this.createMessage(original, this.chat);
    if(message == null) return;

    for(final V viewer : this.viewers) {
      this.chat.sendMessage(viewer, message, type);
    }
  }

  @Override
  public void sendActionBar(final @NonNull Component original) {
    if(this.actionBar == null) return;

    final Object message = this.createMessage(original, this.actionBar);
    if(message == null) return;

    for(final V viewer : this.viewers) {
      this.actionBar.sendMessage(viewer, message);
    }
  }

  @Override
  public void playSound(final net.kyori.adventure.sound.@NonNull Sound original) {
    if(this.sound == null) return;

    for(final V viewer : this.viewers) {
      final Object position = this.sound.createPosition(viewer);
      if(position == null) continue;

      this.sound.playSound(viewer, original, position);
    }
  }

  @Override
  public void playSound(final net.kyori.adventure.sound.@NonNull Sound original, final double x, final double y, final double z) {
    if(this.sound == null) return;

    final Object position = this.sound.createPosition(x, y, z);
    for(final V viewer : this.viewers) {
      this.sound.playSound(viewer, original, position);
    }
  }

  @Override
  public void stopSound(final @NonNull SoundStop original) {
    if(this.sound == null) return;

    for(final V viewer : this.viewers) {
      this.sound.stopSound(viewer, original);
    }
  }

  @Override
  public void openBook(final net.kyori.adventure.inventory.@NonNull Book original) {
    if(this.book == null) return;

    final Object title = this.createMessage(original.title(), this.book);
    final Object author = this.createMessage(original.author(), this.book);
    final List<Object> pages = new LinkedList<>();
    for(final Component originalPage : original.pages()) {
      final Object page = this.createMessage(originalPage, this.book);
      if(page != null) {
        pages.add(page);
      }
    }
    if(title == null || author == null || pages.isEmpty()) return;

    final Object book = this.book.createBook(title, author, pages);
    if(book == null) return;

    for(final V viewer : this.viewers) {
      this.book.openBook(viewer, book);
    }
  }

  @Override
  public void showTitle(final net.kyori.adventure.title.@NonNull Title original) {
    if(this.title == null) return;

    final Object mainTitle = this.createMessage(original.title(), this.title);
    final Object subTitle = this.createMessage(original.subtitle(), this.title);
    final int inTicks = this.title.toTicks(original.times().fadeIn());
    final int stayTicks = this.title.toTicks(original.times().stay());
    final int outTicks = this.title.toTicks(original.times().fadeOut());

    final Object title = this.title.createTitle(mainTitle, subTitle, inTicks, stayTicks, outTicks);
    if(title == null) return;

    for(final V viewer : this.viewers) {
      this.title.showTitle(viewer, title);
    }
  }

  @Override
  public void clearTitle() {
    if(this.title == null) return;

    for(final V viewer : this.viewers) {
      this.title.clearTitle(viewer);
    }
  }

  @Override
  public void resetTitle() {
    if(this.title == null) return;

    for(final V viewer : this.viewers) {
      this.title.resetTitle(viewer);
    }
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    if(this.bossBars == null) return;

    Facet.BossBar<V> listener;
    synchronized(this.bossBars) {
      listener = this.bossBars.get(bar);
      if(listener == null) {
        listener = new FacetBossBarListener<>(this.bossBar.createBossBar(this.viewers), () -> this.locale);
        this.bossBars.put(bar, listener);
      }
    }

    if(listener.isEmpty()) {
      listener.bossBarInitialized(bar);
      bar.addListener(listener);
    }

    for(final V viewer : this.viewers) {
      listener.addViewer(viewer);
    }
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    if(this.bossBars == null) return;

    final Facet.BossBar<V> listener = this.bossBars.get(bar);
    if(listener == null) return;

    for(final V viewer : this.viewers) {
      listener.removeViewer(viewer);
    }

    if(listener.isEmpty() && this.bossBars.remove(bar) != null) {
      bar.removeListener(listener);
      listener.close();
    }
  }

  @Override
  public void close() {
    if(this.bossBars != null) {
      for(final BossBar bar : this.bossBars.keySet()) {
        this.hideBossBar(bar);
      }
      this.bossBars.clear();
    }

    for(final V viewer : this.viewers) {
      this.removeViewer(viewer);
    }
    this.viewers.clear();
  }

  private @Nullable Object createMessage(final @NonNull Component original, final Facet.@NonNull Message<V, Object> facet) {
    final Component message = GlobalTranslator.render(original, this.locale);
    final V viewer = this.viewer;
    return viewer == null ? null : facet.createMessage(viewer, message);
  }
}
