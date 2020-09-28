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
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.AudienceProvider;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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

import static java.util.Objects.requireNonNull;

public abstract class FacetAudienceProvider<V, A extends FacetAudience<V>> implements AudienceProvider, ForwardingAudience {
  private final Audience console;
  private final Audience player;
  private final Map<V, A> viewers;
  private final Map<UUID, A> players;
  private final Set<A> consoles;
  private final A empty;
  private volatile boolean closed;

  public FacetAudienceProvider() {
    this.viewers = new ConcurrentHashMap<>();
    this.players = new ConcurrentHashMap<>();
    this.consoles = new CopyOnWriteArraySet<>();
    this.console = Audience.audience(this.consoles);
    this.player = Audience.audience(this.players.values());
    this.empty = this.createAudience(Collections.emptyList());
    this.closed = false;
  }

  /**
   * Adds a viewer.
   *
   * @param viewer a viewer
   */
  public void addViewer(final @NonNull V viewer) {
    if(this.closed) return;
    final A audience = this.viewers.computeIfAbsent(requireNonNull(viewer, "viewer"),
      v -> this.createAudience(Collections.singletonList(v)));
    final UUID playerId = this.hasId(viewer);
    if(playerId != null) {
      this.players.putIfAbsent(playerId, audience);
    } else if(this.isConsole(viewer)) {
      this.consoles.add(audience);
    }
  }

  /**
   * Removes a viewer.
   *
   * @param viewer a viewer
   */
  public void removeViewer(final @NonNull V viewer) {
    final A audience = this.viewers.remove(viewer);
    if(audience == null) return;
    final UUID playerId = this.hasId(viewer);
    if(playerId != null) {
      this.players.remove(playerId);
    } else if(this.isConsole(viewer)) {
      this.consoles.remove(audience);
    }
    audience.close();
  }

  /**
   * Changes a viewer's locale.
   *
   * @param viewer a viewer
   * @param locale a locale
   */
  public void changeViewer(final @NonNull V viewer, final @NonNull Locale locale) {
    final A audience = this.viewers.get(viewer);
    if(audience != null) {
      audience.changeLocale(locale);
    }
  }

  /**
   * Gets the {@link UUID} of a viewer, if they are a player.
   *
   * @param viewer a viewer
   * @return a player id or {@code null} if not a player
   */
  protected abstract @Nullable UUID hasId(final @NonNull V viewer);

  /**
   * Gets whether a viewer is considered console.
   *
   * @param viewer a viewer
   * @return if the viewer is console
   */
  protected abstract boolean isConsole(final @NonNull V viewer);

  /**
   * Gets whether a viewer has permission.
   *
   * @param viewer a viewer
   * @param permission a permission node
   * @return if the viewer has permission
   */
  protected abstract boolean hasPermission(final @NonNull V viewer, final @NonNull String permission);

  /**
   * Gets whether a viewer is in a world.
   *
   * @param viewer a viewer
   * @param world a world name
   * @return if the viewer is in the world
   */
  protected abstract boolean isInWorld(final @NonNull V viewer, final @NonNull Key world);

  /**
   * Gets whether a viewer is on a server.
   *
   * @param viewer a viewer
   * @param server a server name
   * @return if the viewer is on the server
   */
  protected abstract boolean isOnServer(final @NonNull V viewer, final @NonNull String server);

  /**
   * Creates an audience for a collection of viewers.
   *
   * @param viewers a collection viewers
   * @return an audience
   */
  protected abstract @NonNull A createAudience(final @NonNull Collection<V> viewers);

  @Override
  public @NonNull Iterable<? extends Audience> audiences() {
    return this.viewers.values();
  }

  @Override
  public @NonNull Audience all() {
    return this;
  }

  @Override
  public @NonNull Audience console() {
    return this.console;
  }

  @Override
  public @NonNull Audience players() {
    return this.player;
  }

  @Override
  public @NonNull Audience player(final @NonNull UUID playerId) {
    return this.players.getOrDefault(playerId, this.empty);
  }

  /**
   * Creates an audience based on a viewer predicate.
   *
   * @param predicate a predicate
   * @return an audience
   */
  public @NonNull Audience filter(final @NonNull Predicate<V> predicate) {
    return Audience.audience(filter(this.viewers.entrySet(), entry -> predicate.test(entry.getKey()), Map.Entry::getValue));
  }

  @Override
  public @NonNull Audience permission(final @NonNull String permission) {
    return this.filter(viewer -> this.hasPermission(viewer, permission));
  }

  @Override
  public @NonNull Audience world(final @NonNull Key world) {
    return this.filter(viewer -> this.isInWorld(viewer, world));
  }

  @Override
  public @NonNull Audience server(final @NonNull String serverName) {
    return this.filter(viewer -> this.isOnServer(viewer, serverName));
  }

  @Override
  public void close() {
    this.closed = true;
    for(final V viewer : this.viewers.keySet()) {
      this.removeViewer(viewer);
    }
  }

  /**
   * Return a live filtered view of the input {@link Iterable}.
   *
   * <p>Only elements that match {@code filter} will be returned
   * by {@linkplain Iterator Iterators} provided.</p>
   *
   * <p>Because this is a <em>live</em> view, any changes to the state of
   * the parent {@linkplain Iterable} will be reflected in iterations over
   * the return value.</p>
   *
   * @param input The source iterator
   * @param filter predicate to filter on
   * @param transformer transformer to change the value
   * @param <T> value type
   * @param <V> another value type
   * @return live filtered view
   */
  private static <T, V> @NonNull Iterable<V> filter(final @NonNull Iterable<T> input, final @NonNull Predicate<T> filter, final @NonNull Function<T, V> transformer) {
    return new Iterable<V>() {
      // create a lazy iterator
      // pre-fetches by one output value to determine whether or not we have another value
      // one value will be fetched on iterator creation, and each next value will be
      // fetched after returning the previous value.
      @Override
      public @NonNull Iterator<V> iterator() {
        return new Iterator<V>() {
          private final Iterator<T> parent = input.iterator();
          private V next;

          private void populate() {
            this.next = null;
            while(this.parent.hasNext()) {
              final T next = this.parent.next();
              if(filter.test(next)) {
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
            if(this.next == null) {
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
        for(final T each : input) {
          if(filter.test(each)) {
            action.accept(transformer.apply(each));
          }
        }
      }
    };
  }
}
