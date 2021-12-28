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
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.platform.facet.Knob.logError;
import static net.kyori.adventure.platform.facet.Knob.logMessage;
import static net.kyori.adventure.platform.facet.Knob.logUnsupported;

/**
 * A unit of functionality for a viewer.
 *
 * <p>This is not supported API. Subject to change at any time.</p>
 *
 * @param <V> a viewer type
 * @since 4.0.0
 */
@ApiStatus.Internal
public interface Facet<V> {
  /**
   * Creates a collection of supported facets.
   *
   * @param suppliers an array of facet suppliers
   * @param <V> a viewer type
   * @param <F> a facet type
   * @return a collection of facets
   * @since 4.0.0
   */
  @SafeVarargs
  static <V, F extends Facet<? extends V>> @NotNull Collection<F> of(final @NotNull Supplier<F>... suppliers) {
    final List<F> facets = new LinkedList<>();
    for (final Supplier<F> supplier : suppliers) {
      final F facet;
      try {
        facet = supplier.get();
      } catch (final NoClassDefFoundError error) {
        logMessage("Skipped facet: %s", supplier.getClass().getName());
        continue;
      } catch (final Throwable error) {
        logError(error, "Failed facet: %s", supplier);
        continue;
      }
      if (!facet.isSupported()) {
        logMessage("Skipped facet: %s", facet);
        continue;
      }
      facets.add(facet);
      logMessage("Added facet: %s", facet);
    }
    return facets;
  }

  /**
   * Gets the first applicable facet for a viewer.
   *
   * @param facets a collection of supported facets
   * @param viewer a viewer
   * @param <V> a viewer type
   * @param <F> a facet type
   * @return a facet or {@code null} if none are applicable
   * @since 4.0.0
   */
  static <V, F extends Facet<V>> @Nullable F of(final @Nullable Collection<F> facets, final @Nullable V viewer) {
    if (facets == null || viewer == null) return null;
    for (final F facet : facets) {
      try {
        if (facet.isApplicable(viewer)) {
          logMessage("Selected facet: %s for %s", facet, viewer);
          return facet;
        } else if (Knob.DEBUG) {
          logMessage("Not selecting %s for %s", facet, viewer);
        }
      } catch (final ClassCastException error) {
        if (Knob.DEBUG) {
          logMessage("Exception while getting facet %s for %s: %s", facet, viewer, error.getMessage());
        }
      }
    }
    return null;
  }

  /**
   * Gets whether this handler is supported by the current runtime.
   *
   * <p>If not, this can be discarded since it will fail for all viewers.</p>
   *
   * @return if this handler is supported
   * @since 4.0.0
   */
  default boolean isSupported() {
    return true;
  }

  /**
   * Gets whether this handler is applicable to a particular viewer.
   *
   * <p>This should only be invoked if {@link #isSupported()} is {@code true}.</p>
   *
   * @param viewer a viewer
   * @return if this handler is applicable to a viewer
   * @since 4.0.0
   */
  default boolean isApplicable(final @NotNull V viewer) {
    return true;
  }

  /**
   * A facet that converts components between formats.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   * @since 4.0.0
   */
  interface Message<V, M> extends Facet<V> {
    int PROTOCOL_HEX_COLOR = 713; // Added 20w17a
    int PROTOCOL_JSON = 5; // Added 14w02a

    /**
     * Creates a message.
     *
     * <p>Messages should not be translated to a viewer's locale, this is done elsewhere.</p>
     *
     * @param viewer a viewer
     * @param message a message
     * @return a message or {@code null}
     * @since 4.0.0
     */
    @Nullable M createMessage(final @NotNull V viewer, final @NotNull Component message);
  }

  /**
   * A facet that sends chat messages.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   * @since 4.0.0
   */
  interface Chat<V, M> extends Message<V, M> {
    /**
     * Sends a chat message.
     *
     * @param viewer a viewer
     * @param source the sender's identity
     * @param message a message
     * @param type a message type
     * @since 4.0.0
     */
    void sendMessage(final @NotNull V viewer, final @NotNull Identity source, final @NotNull M message, final @NotNull MessageType type);
  }

  /**
   * A facet that sends chat messages, using packets.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   * @since 4.0.0
   */
  interface ChatPacket<V, M> extends Chat<V, M> {
    byte TYPE_CHAT = 0;
    byte TYPE_SYSTEM = 1;
    byte TYPE_ACTION_BAR = 2;

    /**
     * Creates a message type.
     *
     * @param type a message type
     * @return an ordinal
     * @since 4.0.0
     */
    default byte createMessageType(final @NotNull MessageType type) {
      if (type == MessageType.CHAT) {
        return TYPE_CHAT;
      } else if (type == MessageType.SYSTEM) {
        return TYPE_SYSTEM;
      }
      logUnsupported(this, type);
      return TYPE_CHAT;
    }
  }

  /**
   * A facet that sends action bars.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   * @since 4.0.0
   */
  interface ActionBar<V, M> extends Message<V, M> {
    /**
     * Sends an action bar.
     *
     * @param viewer a viewer
     * @param message a message
     * @since 4.0.0
     */
    void sendMessage(final @NotNull V viewer, final @NotNull M message);
  }

  /**
   * A facet that shows, clears, and resets titles.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   * @param <C> a collection type
   * @param <T> a completed title type
   * @since 4.0.0
   */
  interface Title<V, M, C, T> extends Message<V, M> {
    int PROTOCOL_ACTION_BAR = 310; // Added 16w40a
    long MAX_SECONDS = Long.MAX_VALUE / 20;

    /**
     * Creates a collection that will receive title parts.
     *
     * @return the collection
     * @since 4.0.0
     */
    @NotNull C createTitleCollection();

    /**
     * Contribute a title part to the title builder.
     *
     * <p>This will only be called if a title is present</p>
     *
     * @param coll collection
     * @param title title text
     * @since 4.0.0
     */
    void contributeTitle(final @NotNull C coll, final @NotNull M title);

    /**
     * Contribute a subtitle part to the title builder.
     *
     * <p>This will only be called if a subtitle is present</p>
     *
     * @param coll collection
     * @param subtitle subtitle text
     * @since 4.0.0
     */
    void contributeSubtitle(final @NotNull C coll, final @NotNull M subtitle);

    /**
     * Contribute a times part to the title builder.
     *
     * <p>This will only be called if times are present</p>
     *
     * @param coll collection
     * @param inTicks number of fade in ticks
     * @param stayTicks number of stay ticks
     * @param outTicks number of fade out ticks
     * @since 4.0.0
     */
    void contributeTimes(final @NotNull C coll, final int inTicks, final int stayTicks, final int outTicks);

    /**
     * Complete a title.
     *
     * @param coll The in-progress collection of parts
     * @return a title or {@code null}
     * @since 4.0.0
     */
    @Nullable T completeTitle(final @NotNull C coll);

    /**
     * Shows a title.
     *
     * @param viewer a viewer
     * @param title a title
     * @since 4.0.0
     */
    void showTitle(final @NotNull V viewer, final @NotNull T title);

    /**
     * Clears a title.
     *
     * @param viewer a viewer
     * @since 4.0.0
     */
    void clearTitle(final @NotNull V viewer);

    /**
     * Resets a title.
     *
     * @param viewer a viewer
     * @since 4.0.0
     */
    void resetTitle(final @NotNull V viewer);

    /**
     * Gets the ticks for a duration.
     *
     * @param duration a duration
     * @return the ticks
     * @since 4.0.0
     */
    default int toTicks(final @Nullable Duration duration) {
      if (duration == null || duration.isNegative()) {
        return -1;
      }

      if (duration.getSeconds() > MAX_SECONDS) {
        // TODO: throw an exception here?
        return Integer.MAX_VALUE;
      }

      return (int) (duration.getSeconds() * 20 // 20ticks/sec
        + duration.getNano() / 50_000_000); // 50ms * 1ms/1000000ns
    }
  }

  /**
   * A facet that sends titles, using packets.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   * @param <T> a title type
   * @since 4.0.0
   */
  interface TitlePacket<V, M, C, T> extends Title<V, M, C, T> {
    int ACTION_TITLE = 0;
    int ACTION_SUBTITLE = 1;
    int ACTION_ACTIONBAR = 2;
    int ACTION_TIMES = 3;
    int ACTION_CLEAR = 4;
    int ACTION_RESET = 5;
  }

  /**
   * A facet that requires a 3D vector.
   *
   * @param <V> a viewer type
   * @param <P> a position type
   * @since 4.0.0
   */
  interface Position<V, P> extends Facet<V> {
    /**
     * Finds a viewer's position.
     *
     * @param viewer a viewer
     * @return a position or {@code null} if cannot be found
     * @since 4.0.0
     */
    @Nullable P createPosition(final @NotNull V viewer);

    /**
     * Creates a position.
     *
     * @param x a x coordinate
     * @param y a y coordinate
     * @param z a z coordinate
     * @return a position
     * @since 4.0.0
     */
    @NotNull P createPosition(final double x, final double y, final double z);
  }

  /**
   * A facet that plays and stops sounds.
   *
   * @param <V> a viewer type
   * @param <P> a position type
   * @since 4.0.0
   */
  interface Sound<V, P> extends Position<V, P> {
    /**
     * Plays a sound.
     *
     * @param viewer a viewer
     * @param sound a sound
     * @param position a position
     * @since 4.0.0
     */
    void playSound(final @NotNull V viewer, final net.kyori.adventure.sound.@NotNull Sound sound, final @NotNull P position);

    /**
     * Stops a sound.
     *
     * @param viewer a viewer
     * @param sound a sound stop
     * @since 4.0.0
     */
    void stopSound(final @NotNull V viewer, final @NotNull SoundStop sound);
  }

  /**
   * Create a sound that follows a certain entity.
   *
   * @param <V> viewer type
   * @param <M> sound packet type
   * @since 4.0.0
   */
  interface EntitySound<V, M> extends Facet<V> {
    M createForSelf(final V viewer, final net.kyori.adventure.sound.@NotNull Sound sound);

    M createForEmitter(final net.kyori.adventure.sound.@NotNull Sound sound, final net.kyori.adventure.sound.Sound.@NotNull Emitter emitter);

    void playSound(final @NotNull V viewer, final M message);
  }

  /**
   * A facet that opens a book.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   * @param <B> a book type
   * @since 4.0.0
   */
  interface Book<V, M, B> extends Message<V, M> {
    /**
     * Creates a book.
     *
     * @param title a title
     * @param author an author
     * @param pages a collection of pages
     * @return a book or {@code null}
     * @since 4.0.0
     */
    @Nullable B createBook(final @NotNull String title, final @NotNull String author, final @NotNull Iterable<M> pages);

    /**
     * Opens a book.
     *
     * @param viewer a viewer
     * @param book a book
     * @since 4.0.0
     */
    void openBook(final @NotNull V viewer, final @NotNull B book);
  }

  /**
   * A facet that listens to boss bar changes.
   *
   * @param <V> a viewer type
   * @since 4.0.0
   */
  interface BossBar<V> extends net.kyori.adventure.bossbar.BossBar.Listener, Closeable {
    int PROTOCOL_BOSS_BAR = 356; // Added 18w05a

    /**
     * A builder for boss bar facets.
     *
     * @param <V> a viewer type
     * @param <B> a boss bar type
     * @since 4.0.0
     */
    @FunctionalInterface
    interface Builder<V, B extends BossBar<V>> extends Facet<V> {
      /**
       * Creates a boss bar.
       *
       * @param viewer a viewer
       * @return a boss bar
       * @since 4.0.0
       */
      @NotNull B createBossBar(final @NotNull Collection<V> viewer);
    }

    /**
     * Initializes the boss bar.
     *
     * @param bar a boss bar
     * @since 4.0.0
     */
    default void bossBarInitialized(final net.kyori.adventure.bossbar.@NotNull BossBar bar) {
      this.bossBarNameChanged(bar, bar.name(), bar.name());
      this.bossBarColorChanged(bar, bar.color(), bar.color());
      this.bossBarProgressChanged(bar, bar.progress(), bar.progress());
      this.bossBarFlagsChanged(bar, bar.flags(), Collections.emptySet());
      this.bossBarOverlayChanged(bar, bar.overlay(), bar.overlay());
    }

    /**
     * Adds a viewer to the boss bar.
     *
     * @param viewer a viewer
     * @since 4.0.0
     */
    void addViewer(final @NotNull V viewer);

    /**
     * Removes a viewer from the boss bar.
     *
     * @param viewer a viewer
     * @since 4.0.0
     */
    void removeViewer(final @NotNull V viewer);

    /**
     * Gets whether the boss bar has no viewers.
     *
     * @return if the boss bar is empty
     * @since 4.0.0
     */
    boolean isEmpty();

    /**
     * Removes all viewers.
     *
     * @since 4.0.0
     */
    @Override
    void close();
  }

  /**
   * A facet that listens to boss bar changes, using packets.
   *
   * @param <V> a viewer type
   * @since 4.0.0
   */
  interface BossBarPacket<V> extends BossBar<V> {
    int ACTION_ADD = 0;
    int ACTION_REMOVE = 1;
    int ACTION_HEALTH = 2;
    int ACTION_TITLE = 3;
    int ACTION_STYLE = 4;
    int ACTION_FLAG = 5;

    /**
     * Creates a color.
     *
     * @param color a color
     * @return an ordinal
     * @since 4.0.0
     */
    default int createColor(final net.kyori.adventure.bossbar.BossBar.@NotNull Color color) {
      if (color == net.kyori.adventure.bossbar.BossBar.Color.PURPLE) {
        return 5;
      } else if (color == net.kyori.adventure.bossbar.BossBar.Color.PINK) {
        return 0;
      } else if (color == net.kyori.adventure.bossbar.BossBar.Color.BLUE) {
        return 1;
      } else if (color == net.kyori.adventure.bossbar.BossBar.Color.RED) {
        return 2;
      } else if (color == net.kyori.adventure.bossbar.BossBar.Color.GREEN) {
        return 3;
      } else if (color == net.kyori.adventure.bossbar.BossBar.Color.YELLOW) {
        return 4;
      } else if (color == net.kyori.adventure.bossbar.BossBar.Color.WHITE) {
        return 6;
      }
      logUnsupported(this, color);
      return 5;
    }

    /**
     * Creates an overlay.
     *
     * @param overlay an overlay
     * @return an ordinal
     * @since 4.0.0
     */
    default int createOverlay(final net.kyori.adventure.bossbar.BossBar.@NotNull Overlay overlay) {
      if (overlay == net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS) {
        return 0;
      } else if (overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_6) {
        return 1;
      } else if (overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_10) {
        return 2;
      } else if (overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_12) {
        return 3;
      } else if (overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_20) {
        return 4;
      }
      logUnsupported(this, overlay);
      return 0;
    }

    /**
     * Creates a bit flag.
     *
     * @param flagBit a flag bit
     * @param flagsAdded a set of added flags
     * @param flagsRemoved a set of removed flags
     * @return an ordinal
     * @since 4.0.0
     */
    default byte createFlag(final byte flagBit, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsAdded, final @NotNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsRemoved) {
      byte bit = flagBit;
      for (final net.kyori.adventure.bossbar.BossBar.@NotNull Flag flag : flagsAdded) {
        if (flag == net.kyori.adventure.bossbar.BossBar.Flag.DARKEN_SCREEN) {
          bit |= 1;
        } else if (flag == net.kyori.adventure.bossbar.BossBar.Flag.PLAY_BOSS_MUSIC) {
          bit |= 1 << 1;
        } else if (flag == net.kyori.adventure.bossbar.BossBar.Flag.CREATE_WORLD_FOG) {
          bit |= 1 << 2;
        } else {
          logUnsupported(this, flag);
        }
      }
      for (final net.kyori.adventure.bossbar.BossBar.@NotNull Flag flag : flagsRemoved) {
        if (flag == net.kyori.adventure.bossbar.BossBar.Flag.DARKEN_SCREEN) {
          bit &= ~1;
        } else if (flag == net.kyori.adventure.bossbar.BossBar.Flag.PLAY_BOSS_MUSIC) {
          bit &= ~(1 << 1);
        } else if (flag == net.kyori.adventure.bossbar.BossBar.Flag.CREATE_WORLD_FOG) {
          bit &= ~(1 << 2);
        } else {
          logUnsupported(this, flag);
        }
      }
      return bit;
    }
  }

  /**
   * A facet that listens to boss bar changes, using fake entities.
   *
   * @param <V> a viewer type
   * @param <P> a position type
   * @since 4.0.0
   */
  interface BossBarEntity<V, P> extends BossBar<V>, FakeEntity<V, P> {
    int OFFSET_PITCH = 30;
    int OFFSET_YAW = 0;
    int OFFSET_MAGNITUDE = 40;
    int INVULNERABLE_KEY = 20;
    int INVULNERABLE_TICKS = 890;

    @Override
    default void bossBarProgressChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final float oldProgress, final float newProgress) {
      this.health(newProgress);
    }

    @Override
    default void bossBarNameChanged(final net.kyori.adventure.bossbar.@NotNull BossBar bar, final @NotNull Component oldName, final @NotNull Component newName) {
      this.name(newName);
    }

    @Override
    default void addViewer(final @NotNull V viewer) {
      this.teleport(viewer, this.createPosition(viewer));
    }

    @Override
    default void removeViewer(final @NotNull V viewer) {
      this.teleport(viewer, null);
    }
  }

  /**
   * A facet for spawning client-side entities.
   *
   * @param <V> a viewer type
   * @param <P> a position type
   * @since 4.0.0
   */
  interface FakeEntity<V, P> extends Position<V, P>, Closeable {
    /**
     * Teleports the entity for a viewer.
     *
     * @param viewer a viewer
     * @param position an entity position or {@code null} to remove
     * @since 4.0.0
     */
    void teleport(final @NotNull V viewer, final @Nullable P position);

    /**
     * Sets the entity metadata.
     *
     * @param position a metadata position
     * @param data a value
     * @since 4.0.0
     */
    void metadata(final int position, final @NotNull Object data);

    /**
     * Sets the entity visibility.
     *
     * @param invisible if invisible
     * @since 4.0.0
     */
    void invisible(final boolean invisible);

    /**
     * Sets the entity health.
     *
     * @param health health level, between 0 and 1
     * @since 4.0.0
     */
    void health(final float health);

    /**
     * Sets the entity name.
     *
     * @param name a name
     * @since 4.0.0
     */
    void name(final @NotNull Component name);

    /**
     * Remove the entity for all viewers.
     *
     * @since 4.0.0
     */
    @Override
    void close();
  }

  /**
   * Methods for working with the player tab list.
   *
   * @param <V> viewer
   * @param <M> message type
   * @since 4.0.0
   */
  interface TabList<V, M> extends Message<V, M> {

    /**
     * Update the tab list header and footer.
     *
     * @param header header, null if should be left unchanged
     * @param footer footer, null if should be left unchanged
     * @since 4.0.0
     */
    void send(final V viewer, final @Nullable M header, final @Nullable M footer);
  }

  /**
   * Methods for building pointers.
   *
   * <p>Unlike other {@code Facet}s, pointer facets will stack, so <em>all</em> facets
   * applicable to a particular viewer will be applied.</p>
   *
   * @param <V> the viewer type
   * @since 4.0.0
   */
  interface Pointers<V> extends Facet<V> {
    /**
     * Contribute pointers to the builder for a certain viewer.
     *
     * @param viewer the viewer
     * @param builder the builder
     * @since 4.0.0
     */
    void contributePointers(final V viewer, final net.kyori.adventure.pointer.Pointers.Builder builder);
  }
}
