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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * An audience provider implementation using facets.
 *
 * <p>This is not supported API. Subject to change at any time.</p>
 *
 * @param <V> the viewer type
 * @param <A> the audience type
 * @since 4.0.0
 */
@ApiStatus.Internal
public abstract class FacetAudienceProvider<V, A extends FacetAudience<V>>
  implements AudienceProvider, ForwardingAudience {
  protected static final Locale DEFAULT_LOCALE = Locale.US;
  protected final ComponentRenderer<Pointered> componentRenderer;

  private final Audience console;
  private final Audience player;
  protected final Map<V, A> viewers;
  private final Map<UUID, A> players;
  private final Set<A> consoles;
  private final A empty;
  private volatile boolean closed;

  protected FacetAudienceProvider(final @NotNull ComponentRenderer<Pointered> componentRenderer) {
    this.componentRenderer = requireNonNull(componentRenderer, "component renderer");
    this.viewers = new ConcurrentHashMap<>();
    this.players = new ConcurrentHashMap<>();
    this.consoles = new CopyOnWriteArraySet<>();
    this.console = new ForwardingAudience() {
      @Override
      public @NotNull Iterable<? extends Audience> audiences() {
        return FacetAudienceProvider.this.consoles;
      }

      @Override
      public @NotNull Pointers pointers() {
        if (FacetAudienceProvider.this.consoles.size() == 1) {
          return FacetAudienceProvider.this.consoles.iterator().next().pointers();
        } else {
          return Pointers.empty();
        }
      }
    };
    this.player = Audience.audience(this.players.values());
    this.empty = this.createAudience(Collections.emptyList());
    this.closed = false;
  }

  /**
   * Adds a viewer.
   *
   * @param viewer a viewer
   * @since 4.0.0
   */
  public void addViewer(final @NotNull V viewer) {
    if (this.closed) return;
    final A audience = this.viewers.computeIfAbsent(
        requireNonNull(viewer, "viewer"),
        v -> this.createAudience(Collections.singletonList(v)));
    final FacetPointers.Type type = audience.getOrDefault(FacetPointers.TYPE, FacetPointers.Type.OTHER);
    if (type == FacetPointers.Type.PLAYER) {
      final @Nullable UUID id = audience.getOrDefault(Identity.UUID, null);
      if (id != null) this.players.putIfAbsent(id, audience);
    } else if (type == FacetPointers.Type.CONSOLE) {
      this.consoles.add(audience);
    }
  }

  /**
   * Removes a viewer.
   *
   * @param viewer a viewer
   * @since 4.0.0
   */
  public void removeViewer(final @NotNull V viewer) {
    final A audience = this.viewers.remove(viewer);
    if (audience == null) return;
    final FacetPointers.Type type = audience.getOrDefault(FacetPointers.TYPE, FacetPointers.Type.OTHER);
    if (type == FacetPointers.Type.PLAYER) {
      final @Nullable UUID id = audience.getOrDefault(Identity.UUID, null);
      if (id != null) this.players.remove(id);
    } else if (type == FacetPointers.Type.CONSOLE) {
      this.consoles.remove(audience);
    }
    audience.close();
  }

  /**
   * Refreshes a viewer's metadata.
   *
   * <p>Should be called after a viewer changes their locale, world, server, etc.</p>
   *
   * @param viewer a viewer
   * @since 4.0.0
   */
  public void refreshViewer(final @NotNull V viewer) {
    final A audience = this.viewers.get(viewer);
    if (audience != null) {
      audience.refresh();
    }
  }

  /**
   * Creates an audience for a collection of viewers.
   *
   * @param viewers a collection viewers
   * @return an audience
   */
  protected abstract @NotNull A createAudience(final @NotNull Collection<V> viewers);

  @Override
  public @NotNull Iterable<? extends Audience> audiences() {
    return this.viewers.values();
  }

  @Override
  public @NotNull Audience all() {
    return this;
  }

  @Override
  public @NotNull Audience console() {
    return this.console;
  }

  @Override
  public @NotNull Audience players() {
    return this.player;
  }

  @Override
  public @NotNull Audience player(final @NotNull UUID playerId) {
    return this.players.getOrDefault(playerId, this.empty);
  }

  /**
   * Creates an audience based on a viewer predicate.
   *
   * @param predicate a predicate
   * @return an audience
   * @since 4.0.0
   */
  public @NotNull Audience filter(final @NotNull Predicate<V> predicate) {
    return Audience.audience(
      filter(
        this.viewers.entrySet(), entry -> predicate.test(entry.getKey()), Map.Entry::getValue));
  }

  private @NotNull Audience filterPointers(final @NotNull Predicate<Pointered> predicate) {
    return Audience.audience(
      filter(
        this.viewers.entrySet(),
        entry -> predicate.test(entry.getValue()),
        Map.Entry::getValue));
  }

  @Override
  public @NotNull Audience permission(final @NotNull String permission) {
    return this.filterPointers(pointers -> pointers.get(PermissionChecker.POINTER).orElse(PermissionChecker.always(TriState.FALSE)).test(permission));
  }

  @Override
  public @NotNull Audience world(final @NotNull Key world) {
    return this.filterPointers(pointers -> world.equals(pointers.getOrDefault(FacetPointers.WORLD, null)));
  }

  @Override
  public @NotNull Audience server(final @NotNull String serverName) {
    return this.filterPointers(pointers -> serverName.equals(pointers.getOrDefault(FacetPointers.SERVER, null)));
  }

  @Override
  public void close() {
    this.closed = true;
    for (final V viewer : this.viewers.keySet()) {
      this.removeViewer(viewer);
    }
  }

  /**
   * Return a live filtered view of the input {@link Iterable}.
   *
   * <p>Only elements that match {@code filter} will be returned by {@linkplain Iterator Iterators}
   * provided.</p>
   *
   * <p>Because this is a <em>live</em> view, any changes to the state of the parent
   * {@linkplain Iterable} will be reflected in iterations over the return value.</p>
   *
   * @param input The source iterator
   * @param filter predicate to filter on
   * @param transformer transformer to change the value
   * @param <T> value type
   * @param <V> another value type
   * @return live filtered view
   */
  private static <T, V> @NotNull Iterable<V> filter(final @NotNull Iterable<T> input, final @NotNull Predicate<T> filter, final @NotNull Function<T, V> transformer) {
    return new Iterable<V>() {
      // create a lazy iterator
      // pre-fetches by one output value to determine whether or not we have another value
      // one value will be fetched on iterator creation, and each next value will be
      // fetched after returning the previous value.
      @Override
      public @NotNull Iterator<V> iterator() {
        return new Iterator<V>() {
          private final Iterator<T> parent = input.iterator();
          private V next;

          private void populate() {
            this.next = null;
            while (this.parent.hasNext()) {
              final T next = this.parent.next();
              if (filter.test(next)) {
                this.next = transformer.apply(next);
                return;
              }
            }
          }

          // initialize first value
          {
            this.populate();
          }

          @Override
          public boolean hasNext() {
            return this.next != null;
          }

          @Override
          public V next() {
            if (this.next == null) {
              throw new NoSuchElementException();
            }
            final V next = this.next;
            this.populate();
            return next;
          }
        };
      }

      @Override
      public void forEach(final Consumer<? super V> action) {
        for (final T each : input) {
          if (filter.test(each)) {
            action.accept(transformer.apply(each));
          }
        }
      }
    };
  }
}
