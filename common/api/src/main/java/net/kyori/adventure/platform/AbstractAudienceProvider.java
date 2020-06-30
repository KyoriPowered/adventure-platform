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
package net.kyori.adventure.platform;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MultiAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * A base implementation of {@link AudienceProvider} on a given platform.
 */
public abstract class AbstractAudienceProvider<A extends Audience & AudienceInfo> implements AudienceProvider {

  private Audience all;
  private Audience console;
  private Audience players;
  private Map<UUID, A> playerMap;
  private Set<A> senderSet;
  private Map<String, Audience> permissionMap;
  private Map<Key, Audience> worldMap;
  private Map<String, Audience> serverMap;
  private ComponentRenderer<AudienceInfo> renderer;
  private volatile boolean closed;

  protected AbstractAudienceProvider() {
    this.senderSet = ConcurrentHashMap.newKeySet();
    this.all = (MultiAudience) () -> this.senderSet;
    this.playerMap = new ConcurrentHashMap<>();
    this.players = (MultiAudience) () -> this.playerMap.values();
    this.console = new ConsoleAudience();
    this.permissionMap = new ConcurrentHashMap<>();
    this.worldMap = new ConcurrentHashMap<>();
    this.serverMap = new ConcurrentHashMap<>();
    this.renderer = new ComponentRenderer<AudienceInfo>() {
      @Override
      public @NonNull Component render(@NonNull Component component, @NonNull AudienceInfo info) {
        return component; // TODO: allow this to be customized
      }
    };
    this.closed = false;
  }

  /**
   * Adds an audience to the registry.
   *
   * @param audience an audience
   */
  protected void add(A audience) {
    if (closed) return;

    this.senderSet.add(audience);
    if(audience.isPlayer()) {
      this.playerMap.put(audience.getId(), audience);
    }
  }

  /**
   * Removes an audience from the registry.
   *
   * @param playerId a player id
   */
  protected void remove(UUID playerId) {
    final A removed = this.playerMap.remove(playerId);
    this.senderSet.remove(removed);
  }

  @Override
  public @NonNull Audience all() {
    return this.all;
  }

  private class ConsoleAudience implements MultiAudience {
    private final Iterable<A> console = filter(senderSet, A::isConsole);
    @Override
    public @NonNull Iterable<? extends Audience> audiences() {
      return this.console;
    }
  }

  @Override
  public @NonNull Audience console() {
    return this.console;
  }

  @Override
  public @NonNull Audience players() {
    return this.players;
  }

  @Override
  public @NonNull Audience player(@NonNull UUID playerId) {
    final A player = this.playerMap.get(playerId);
    return player == null ? Audience.empty() : player;
  }

  private class PermissionAudience implements MultiAudience {
    private final Iterable<A> filtered = filter(senderSet, this::hasPermission);
    private final String permission;

    private PermissionAudience(final @NonNull String permission) {
      this.permission = requireNonNull(permission, "permission");
    }

    private boolean hasPermission(A audience) {
      return audience.hasPermission(this.permission);
    }

    @Override
    public @NonNull Iterable<? extends Audience> audiences() {
      return this.filtered;
    }
  }

  @Override
  public @NonNull Audience permission(@NonNull String permission) {
    // TODO: potential memory leak, can we limit collection size somehow?
    // the rest of the collections could run into the same issue, but this one presents the most potential for unbounded growth
    // maybe don't even cache, ask ppl to hold references?
    return this.permissionMap.computeIfAbsent(permission, PermissionAudience::new);
  }

  private class WorldAudience implements MultiAudience {
    private final Iterable<A> filtered = filter(playerMap.values(), this::inWorld);
    private final Key world;

    private WorldAudience(final @NonNull Key world) {
      this.world = requireNonNull(world, "world id");
    }

    private boolean inWorld(A audience) {
      return this.world.equals(audience.getWorld());
    }

    @Override
    public @NonNull Iterable<? extends Audience> audiences() {
      return this.filtered;
    }
  }

  @Override
  public @NonNull Audience world(@NonNull Key world) {
    return this.worldMap.computeIfAbsent(world, WorldAudience::new);
  }

  private class ServerAudience implements MultiAudience {
    private final Iterable<A> filtered = filter(playerMap.values(), this::isOnServer);
    private final String serverName;

    private ServerAudience(final @NonNull String serverName) {
      this.serverName = requireNonNull(serverName, "server name");
    }

    private boolean isOnServer(A audience) {
      return this.serverName.equals(audience.getServer());
    }

    @Override
    public @NonNull Iterable<? extends Audience> audiences() {
      return this.filtered;
    }
  }

  @Override
  public @NonNull Audience server(@NonNull String serverName) {
    return this.serverMap.computeIfAbsent(serverName, ServerAudience::new);
  }

  @Override
  public @NonNull ComponentRenderer<AudienceInfo> renderer() {
    return this.renderer;
  }

  @Override
  public void close() {
    if (!this.closed) {
      this.closed = true;
      this.all = Audience.empty();
      this.console = Audience.empty();
      this.players = Audience.empty();
      this.playerMap = Collections.emptyMap();
      this.senderSet = Collections.emptySet();
      this.permissionMap = Collections.emptyMap();
      this.worldMap = Collections.emptyMap();
      this.serverMap = Collections.emptyMap();
      // TODO: this.renderer = no-op
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
   * @param filter Predicate to filter on
   * @param <T> value type
   * @return live filtered view
   */
  private static <T> Iterable<T> filter(final Iterable<T> input, Predicate<T> filter) {
    return new Iterable<T>() {
      // create a lazy iterator
      // pre-fetches by one output value to determine whether or not we have another value
      // one value will be fetched on iterator creation, and each next value will be
      // fetched after returning the previous value.
      @Override
      public @NonNull Iterator<T> iterator() {
        return new Iterator<T>() {
          private final Iterator<T> parent = input.iterator();
          private T next;

          private void populate() {
            this.next = null;
            while(this.parent.hasNext()) {
              T next = this.parent.next();
              if(filter.test(next)) {
                this.next = next;
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
          public T next() {
            if(this.next == null) {
              throw new NoSuchElementException();
            }
            T next = this.next;
            this.populate();
            return next;
          }
        };
      }

      @Override
      public void forEach(final Consumer<? super T> action) {
        for(final T each : input) {
          if(filter.test(each)) action.accept(each);
        }
      }
    };
  }
}
