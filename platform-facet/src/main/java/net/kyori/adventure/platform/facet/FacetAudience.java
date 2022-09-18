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

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import static java.util.Objects.requireNonNull;

/**
 * An {@link Audience} that is implemented by {@link Facet}s.
 *
 * <p>This audience must support multiple viewers, although platforms do not use this feature yet.</p>
 *
 * <p>This is not supported API. Subject to change at any time.</p>
 *
 * @param <V> a viewer type
 * @see Facet
 * @since 4.0.0
 */
@ApiStatus.Internal
public class FacetAudience<V> implements Audience, Closeable {
  protected final @NotNull FacetAudienceProvider<V, FacetAudience<V>> provider;

  private final @NotNull Set<V> viewers;
  private @Nullable V viewer;
  private volatile Pointers pointers; // lazy init

  private final Facet.@Nullable Chat<V, Object> chat;
  private final Facet.@Nullable ActionBar<V, Object> actionBar;
  private final Facet.@Nullable Title<V, Object, Object, Object> title;
  private final Facet.@Nullable Sound<V, Object> sound;
  private final Facet.@Nullable EntitySound<V, Object> entitySound;
  private final Facet.@Nullable Book<V, Object, Object> book;
  private final Facet.BossBar.@Nullable Builder<V, Facet.BossBar<V>> bossBar;
  private final @Nullable Map<BossBar, Facet.BossBar<V>> bossBars;
  private final Facet.@Nullable TabList<V, Object> tabList;
  private final @NotNull Collection<? extends Facet.Pointers<V>> pointerProviders;

  /**
   * Create a new facet-based audience.
   *
   * @param provider for this audience
   * @param viewers the viewers receiving content sent to this audience
   * @param chat chat facet candidates
   * @param actionBar action bar facet candidates
   * @param title title facet candidates
   * @param sound sound facet candidates
   * @param entitySound entity sound facet candidates
   * @param book book facet candidates
   * @param bossBar boss bar facet candidates
   * @param tabList tab list facet candidates
   * @param pointerProviders facets that provide pointers to this audience
   * @since 4.0.0
   */
  @SuppressWarnings({
    "unchecked",
    "rawtypes"
  }) // Without suppression, this constructor becomes unreadable
  public FacetAudience(
    final @NotNull FacetAudienceProvider provider,
    final @NotNull Collection<? extends V> viewers,
    final @Nullable Collection<? extends Facet.Chat> chat,
    final @Nullable Collection<? extends Facet.ActionBar> actionBar,
    final @Nullable Collection<? extends Facet.Title> title,
    final @Nullable Collection<? extends Facet.Sound> sound,
    final @Nullable Collection<? extends Facet.EntitySound> entitySound,
    final @Nullable Collection<? extends Facet.Book> book,
    final @Nullable Collection<? extends Facet.BossBar.Builder> bossBar,
    final @Nullable Collection<? extends Facet.TabList> tabList,
    final @Nullable Collection<? extends Facet.Pointers> pointerProviders
  ) {
    this.provider = requireNonNull(provider, "audience provider");
    this.viewers = new CopyOnWriteArraySet<>();
    for (final V viewer : requireNonNull(viewers, "viewers")) {
      this.addViewer(viewer);
    }
    this.refresh();
    this.chat = Facet.of(chat, this.viewer);
    this.actionBar = Facet.of(actionBar, this.viewer);
    this.title = Facet.of(title, this.viewer);
    this.sound = Facet.of(sound, this.viewer);
    this.entitySound = Facet.of(entitySound, this.viewer);
    this.book = Facet.of(book, this.viewer);
    this.bossBar = Facet.of(bossBar, this.viewer);
    this.bossBars =
      this.bossBar == null ? null : Collections.synchronizedMap(new IdentityHashMap<>(4));
    this.tabList = Facet.of(tabList, this.viewer);
    this.pointerProviders = pointerProviders == null ? Collections.emptyList() : (Collection) pointerProviders;
  }

  /**
   * Add a member to this audience.
   *
   * @param viewer the viewer
   * @since 4.0.0
   */
  public void addViewer(final @NotNull V viewer) {
    if (this.viewers.add(viewer) && this.viewer == null) {
      this.viewer = viewer;
      this.refresh();
    }
  }

  /**
   * Remove a viewer from this audience.
   *
   * @param viewer the viewer to remove
   * @since 4.0.0
   */
  public void removeViewer(final @NotNull V viewer) {
    if (this.viewers.remove(viewer) && this.viewer == viewer) {
      this.viewer = this.viewers.isEmpty() ? null : this.viewers.iterator().next();
      this.refresh();
    }

    if (this.bossBars == null) return;
    for (final Facet.BossBar<V> listener : this.bossBars.values()) {
      listener.removeViewer(viewer);
    }
  }

  /**
   * Refresh the audience.
   *
   * @since 4.0.0
   */
  public void refresh() {
    synchronized (this) {
      this.pointers = null; // todo: is this necessary?
    }

    if (this.bossBars == null) return;
    for (final Map.Entry<BossBar, Facet.BossBar<V>> entry : this.bossBars.entrySet()) {
      final BossBar bar = entry.getKey();
      final Facet.BossBar<V> listener = entry.getValue();
      // Since boss bars persist through a refresh, the titles must be re-rendered
      listener.bossBarNameChanged(bar, bar.name(), bar.name());
    }
  }

  @Override
  public void sendMessage(final @NotNull Identity source, final @NotNull Component original, final @NotNull MessageType type) {
    if (this.chat == null) return;

    final Object message = this.createMessage(original, this.chat);
    if (message == null) return;

    for (final V viewer : this.viewers) {
      this.chat.sendMessage(viewer, source, message, type);
    }
  }

  @Override
  public void sendActionBar(final @NotNull Component original) {
    if (this.actionBar == null) return;

    final Object message = this.createMessage(original, this.actionBar);
    if (message == null) return;

    for (final V viewer : this.viewers) {
      this.actionBar.sendMessage(viewer, message);
    }
  }

  @Override
  public void playSound(final net.kyori.adventure.sound.@NotNull Sound original) {
    if (this.sound == null) return;

    for (final V viewer : this.viewers) {
      final Object position = this.sound.createPosition(viewer);
      if (position == null) continue;

      this.sound.playSound(viewer, original, position);
    }
  }

  @Override
  public void playSound(final @NotNull Sound sound, final Sound.@NotNull Emitter emitter) {
    if (this.entitySound == null) return;
    if (emitter == Sound.Emitter.self()) {
      for (final V viewer : this.viewers) {
        final Object message = this.entitySound.createForSelf(viewer, sound);
        if (message == null) continue;
        this.entitySound.playSound(viewer, message);
      }

    } else {
      final Object message = this.entitySound.createForEmitter(sound, emitter);
      if (message == null) return;
      for (final V viewer : this.viewers) {
        this.entitySound.playSound(viewer, message);
      }
    }
  }

  @Override
  public void playSound(final net.kyori.adventure.sound.@NotNull Sound original, final double x, final double y, final double z) {
    if (this.sound == null) return;

    final Object position = this.sound.createPosition(x, y, z);
    for (final V viewer : this.viewers) {
      this.sound.playSound(viewer, original, position);
    }
  }

  @Override
  public void stopSound(final @NotNull SoundStop original) {
    if (this.sound == null) return;

    for (final V viewer : this.viewers) {
      this.sound.stopSound(viewer, original);
    }
  }

  @Override
  public void openBook(final net.kyori.adventure.inventory.@NotNull Book original) {
    if (this.book == null) return;

    final String title = this.toPlain(original.title());
    final String author = this.toPlain(original.author());
    final List<Object> pages = new LinkedList<>();
    for (final Component originalPage : original.pages()) {
      final Object page = this.createMessage(originalPage, this.book);
      if (page != null) {
        pages.add(page);
      }
    }
    if (title == null || author == null || pages.isEmpty()) return;

    final Object book = this.book.createBook(title, author, pages);
    if (book == null) return;

    for (final V viewer : this.viewers) {
      this.book.openBook(viewer, book);
    }
  }

  private String toPlain(final Component comp) {
    if (comp == null) {
      return null;
    }
    final StringBuilder builder = new StringBuilder();
    ComponentFlattener.basic().flatten(this.provider.componentRenderer.render(comp, this), builder::append);
    return builder.toString();
  }

  @Override
  public void showTitle(final net.kyori.adventure.title.@NotNull Title original) {
    if (this.title == null) return;

    final Object mainTitle = this.createMessage(original.title(), this.title);
    final Object subTitle = this.createMessage(original.subtitle(), this.title);
    final Title.@Nullable Times times = original.times();
    final int inTicks = times == null ? -1 : this.title.toTicks(times.fadeIn());
    final int stayTicks = times == null ? -1 : this.title.toTicks(times.stay());
    final int outTicks = times == null ? -1 : this.title.toTicks(times.fadeOut());

    final Object collection = this.title.createTitleCollection();
    if (inTicks != -1 || stayTicks != -1 || outTicks != -1) {
      this.title.contributeTimes(collection, inTicks, stayTicks, outTicks);
    }
    this.title.contributeSubtitle(collection, subTitle);
    this.title.contributeTitle(collection, mainTitle);
    final Object title = this.title.completeTitle(collection);
    if (title == null) return;

    for (final V viewer : this.viewers) {
      this.title.showTitle(viewer, title);
    }
  }

  @Override
  public <T> void sendTitlePart(final @NotNull TitlePart<T> part, @NotNull final T value) {
    if (this.title == null) return;

    Objects.requireNonNull(value, "value");
    final Object collection = this.title.createTitleCollection();
    if (part == TitlePart.TITLE) {
      final @Nullable Object message = this.createMessage((Component) value, this.title);
      if (message != null) this.title.contributeTitle(collection, message);
    } else if (part == TitlePart.SUBTITLE) {
      final @Nullable Object message = this.createMessage((Component) value, this.title);
      if (message != null) this.title.contributeSubtitle(collection, message);
    } else if (part == TitlePart.TIMES) {
      final Title.Times times = (Title.Times) value;
      final int inTicks = this.title.toTicks(times.fadeIn());
      final int stayTicks = this.title.toTicks(times.stay());
      final int outTicks = this.title.toTicks(times.fadeOut());
      if (inTicks != -1 || stayTicks != -1 || outTicks != -1) {
        this.title.contributeTimes(collection, inTicks, stayTicks, outTicks);
      }
    } else {
      throw new IllegalArgumentException("Unknown TitlePart '" + part + "'");
    }

    final Object title = this.title.completeTitle(collection);
    if (title == null) return;

    for (final V viewer : this.viewers) {
      this.title.showTitle(viewer, title);
    }
  }

  @Override
  public void clearTitle() {
    if (this.title == null) return;

    for (final V viewer : this.viewers) {
      this.title.clearTitle(viewer);
    }
  }

  @Override
  public void resetTitle() {
    if (this.title == null) return;

    for (final V viewer : this.viewers) {
      this.title.resetTitle(viewer);
    }
  }

  @Override
  public void showBossBar(final @NotNull BossBar bar) {
    if (this.bossBar == null || this.bossBars == null) return;

    Facet.BossBar<V> listener;
    synchronized(this.bossBars) {
      listener = this.bossBars.get(bar);
      if (listener == null) {
        listener =
          new FacetBossBarListener<>(
            this.bossBar.createBossBar(this.viewers),
            message -> this.provider.componentRenderer.render(message, this));
        this.bossBars.put(bar, listener);
      }
    }

    if (listener.isEmpty()) {
      listener.bossBarInitialized(bar);
      bar.addListener(listener);
    }

    for (final V viewer : this.viewers) {
      listener.addViewer(viewer);
    }
  }

  @Override
  public void hideBossBar(final @NotNull BossBar bar) {
    if (this.bossBars == null) return;

    final Facet.BossBar<V> listener = this.bossBars.get(bar);
    if (listener == null) return;

    for (final V viewer : this.viewers) {
      listener.removeViewer(viewer);
    }

    if (listener.isEmpty() && this.bossBars.remove(bar) != null) {
      bar.removeListener(listener);
      listener.close();
    }
  }

  /**
   * Check if this audience has been shown this BossBar
   * @since 4.1.3
   */
  public boolean hasBossBar(final @NotNull BossBar bar) {
    return this.bossBars != null && this.bossBars.containsKey(bar);
  }

  /**
   * Returns a set containing all BossBars this Audience has been shown
   * @since 4.1.3
   */
  public @NotNull @UnmodifiableView Set<BossBar> getBossBars() {
    if (this.bossBars == null) return Collections.emptySet();
    return Collections.unmodifiableSet(this.bossBars.keySet());
  }

  @Override
  public void sendPlayerListHeader(final @NotNull Component header) {
    if (this.tabList != null) {
      final Object headerFormatted = this.createMessage(header, this.tabList);
      if (headerFormatted == null) return;
      for (final V viewer : this.viewers) {
        this.tabList.send(viewer, headerFormatted, null);
      }
    }
  }

  @Override
  public void sendPlayerListFooter(final @NotNull Component footer) {
    if (this.tabList != null) {
      final Object footerFormatted = this.createMessage(footer, this.tabList);
      if (footerFormatted == null) return;
      for (final V viewer : this.viewers) {
        this.tabList.send(viewer, null, footerFormatted);
      }
    }
  }

  @Override
  public void sendPlayerListHeaderAndFooter(final @NotNull Component header, final @NotNull Component footer) {
    if (this.tabList != null) {
      final Object headerFormatted = this.createMessage(header, this.tabList);
      final Object footerFormatted = this.createMessage(footer, this.tabList);
      if (headerFormatted == null || footerFormatted == null) return;

      for (final V viewer : this.viewers) {
        this.tabList.send(viewer, headerFormatted, footerFormatted);
      }
    }
  }

  @Override
  public @NotNull Pointers pointers() {
    if (this.pointers == null) {
      synchronized (this) {
        if (this.pointers == null) {
          final V viewer = this.viewer;
          if (viewer == null) return Pointers.empty();
          final Pointers.Builder builder = Pointers.builder();
          // audience-specific, for things -platform needs to track itself
          this.contributePointers(builder);

          // Then any extra pointers
          for (final Facet.Pointers<V> provider : this.pointerProviders) {
            if (provider.isApplicable(viewer)) {
              provider.contributePointers(viewer, builder);
            }
          }
          return this.pointers = builder.build();
        }
      }
    }

    return this.pointers;
  }

  @ApiStatus.OverrideOnly
  protected void contributePointers(final Pointers.Builder builder) {
  }

  @Override
  public void close() {
    if (this.bossBars != null) {
      for (final BossBar bar : new LinkedList<>(this.bossBars.keySet())) {
        this.hideBossBar(bar);
      }
      this.bossBars.clear();
    }

    for (final V viewer : this.viewers) {
      this.removeViewer(viewer);
    }
    this.viewers.clear();
  }

  private @Nullable Object createMessage(final @NotNull Component original, final Facet.@NotNull Message<V, Object> facet) {
    final Component message = this.provider.componentRenderer.render(original, this);
    final V viewer = this.viewer;
    return viewer == null ? null : facet.createMessage(viewer, message);
  }
}
