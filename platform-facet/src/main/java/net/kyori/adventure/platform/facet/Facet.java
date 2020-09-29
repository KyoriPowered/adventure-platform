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

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static net.kyori.adventure.platform.facet.Knob.logError;
import static net.kyori.adventure.platform.facet.Knob.logMessage;
import static net.kyori.adventure.platform.facet.Knob.logUnsupported;

/**
 * A unit of functionality for a viewer.
 *
 * @param <V> a viewer type
 */
public interface Facet<V> {
  /**
   * Creates a collection of supported facets.
   *
   * @param suppliers an array of facet suppliers
   * @param <V> a viewer type
   * @param <F> a facet type
   * @return a collection of facets
   */
  @SafeVarargs
  static <V, F extends Facet<? extends V>> @NonNull Collection<F> of(final @NonNull Supplier<F>... suppliers) {
    final List<F> facets = new LinkedList<>();
    for(final Supplier<F> supplier : suppliers) {
      final F facet;
      try {
        facet = supplier.get();
      } catch(final NoClassDefFoundError error) {
        logMessage("Skipped facet: %s", supplier.getClass().getName());
        continue;
      } catch(final Throwable error) {
        logError(error, "Failed facet: %s", supplier);
        continue;
      }
      if(!facet.isSupported()) {
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
   */
  static <V, F extends Facet<V>> @Nullable F of(final @Nullable Collection<F> facets, final @Nullable V viewer) {
    if(facets == null || viewer == null) return null;
    for(final F facet : facets) {
      try {
        if(facet.isApplicable(viewer)) {
          logMessage("Selected facet: %s for %s", facet, viewer);
          return facet;
        }
      } catch(final ClassCastException error) {
        // Continue along
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
   */
  default boolean isApplicable(final @NonNull V viewer) {
    return true;
  }

  /**
   * A facet that converts components between formats.
   *
   * @param <V> a viewer type
   * @param <M> a message type
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
     */
    @Nullable M createMessage(final @NonNull V viewer, final @NonNull Component message);
  }

  /**
   * A facet that sends chat messages.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   */
  interface Chat<V, M> extends Message<V, M> {
    /**
     * Sends a chat message.
     *
     * @param viewer a viewer
     * @param message a message
     * @param type a message type
     */
    void sendMessage(final @NonNull V viewer, final @NonNull M message, final @NonNull MessageType type);
  }

  /**
   * A facet that sends chat messages, using packets.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   */
  interface ChatPacket<V, M> extends Chat<V, M> {
    byte TYPE_CHAT = 0;
    byte TYPE_SYSTEM = 1;
    byte TYPE_ACTION_BAR = 2;
    UUID SENDER_NULL = new UUID(0, 0);

    /**
     * Creates a message type.
     *
     * @param type a message type
     * @return an ordinal
     */
    default byte createMessageType(final @NonNull MessageType type) {
      if(type == MessageType.CHAT) {
        return TYPE_CHAT;
      } else if(type == MessageType.SYSTEM) {
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
   */
  interface ActionBar<V, M> extends Message<V, M> {
    /**
     * Sends an action bar.
     *
     * @param viewer a viewer
     * @param message a message
     */
    void sendMessage(final @NonNull V viewer, final @NonNull M message);
  }

  /**
   * A facet that shows, clears, and resets titles.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   * @param <T> a title type
   */
  interface Title<V, M, T> extends Message<V, M> {
    int PROTOCOL_ACTION_BAR = 310; // Added 16w40a

    /**
     * Creates a title.
     *
     * @param title a title or {@code null} if empty
     * @param subTitle a subtitle or {@code null} if empty
     * @param inTicks number of fade in ticks
     * @param stayTicks number of stay ticks
     * @param outTicks number of fade out ticks
     * @return a title or {@code null}
     */
    @Nullable T createTitle(final @Nullable M title, final @Nullable M subTitle, final int inTicks, final int stayTicks, final int outTicks);

    /**
     * Shows a title.
     *
     * @param viewer a viewer
     * @param title a title
     */
    void showTitle(final @NonNull V viewer, final @NonNull T title);

    /**
     * Clears a title.
     *
     * @param viewer a viewer
     */
    void clearTitle(final @NonNull V viewer);

    /**
     * Resets a title.
     *
     * @param viewer a viewer
     */
    void resetTitle(final @NonNull V viewer);

    /**
     * Gets the ticks for a duration.
     *
     * @param duration a duration
     * @return the ticks
     */
    default int toTicks(final @Nullable Duration duration) {
      if(duration == null || duration.isNegative()) {
        return -1;
      }
      return (int) (duration.toMillis() / 50.0);
    }
  }

  /**
   * A facet that sends titles, using packets.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   * @param <T> a title type
   */
  interface TitlePacket<V, M, T> extends Title<V, M, T> {
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
   */
  interface Position<V, P> extends Facet<V> {
    /**
     * Finds a viewer's position.
     *
     * @param viewer a viewer
     * @return a position or {@code null} if cannot be found
     */
    @Nullable P createPosition(final @NonNull V viewer);

    /**
     * Creates a position.
     *
     * @param x a x coordinate
     * @param y a y coordinate
     * @param z a z coordinate
     * @return a position
     */
    @NonNull P createPosition(final double x, final double y, final double z);
  }

  /**
   * A facet that plays and stops sounds.
   *
   * @param <V> a viewer type
   * @param <P> a position type
   */
  interface Sound<V, P> extends Position<V, P> {
    /**
     * Plays a sound.
     *
     * @param viewer a viewer
     * @param sound a sound
     * @param position a position
     */
    void playSound(final @NonNull V viewer, final net.kyori.adventure.sound.@NonNull Sound sound, final @NonNull P position);

    /**
     * Stops a sound.
     *
     * @param viewer a viewer
     * @param sound a sound stop
     */
    void stopSound(final @NonNull V viewer, final @NonNull SoundStop sound);
  }

  /**
   * A facet that opens a book.
   *
   * @param <V> a viewer type
   * @param <M> a message type
   * @param <B> a book type
   */
  interface Book<V, M, B> extends Message<V, M> {
    /**
     * Creates a book.
     *
     * @param title a title
     * @param author an author
     * @param pages a collection of pages
     * @return a book or {@code null}
     */
    @Nullable B createBook(final @NonNull M title, final @NonNull M author, final @NonNull Iterable<M> pages);

    /**
     * Opens a book.
     *
     * @param viewer a viewer
     * @param book a book
     */
    void openBook(final @NonNull V viewer, final @NonNull B book);
  }

  /**
   * A facet that listens to boss bar changes.
   *
   * @param <V> a viewer type
   */
  interface BossBar<V> extends net.kyori.adventure.bossbar.BossBar.Listener, Closeable {
    int PROTOCOL_BOSS_BAR = 356; // Added 18w05a

    /**
     * A builder for boss bar facets.
     *
     * @param <V> a viewer type
     * @param <B> a boss bar type
     */
    @FunctionalInterface
    interface Builder<V, B extends BossBar<V>> extends Facet<V> {
      /**
       * Creates a boss bar.
       *
       * @param viewer a viewer
       * @return a boss bar
       */
      @NonNull B createBossBar(final @NonNull Collection<V> viewer);
    }

    /**
     * Initializes the boss bar.
     *
     * @param bar a boss bar
     */
    default void bossBarInitialized(final net.kyori.adventure.bossbar.@NonNull BossBar bar) {
      this.bossBarNameChanged(bar, bar.name(), bar.name());
      this.bossBarColorChanged(bar, bar.color(), bar.color());
      this.bossBarPercentChanged(bar, bar.percent(), bar.percent());
      this.bossBarFlagsChanged(bar, bar.flags(), Collections.emptySet());
      this.bossBarOverlayChanged(bar, bar.overlay(), bar.overlay());
    }

    /**
     * Adds a viewer to the boss bar.
     *
     * @param viewer a viewer
     */
    void addViewer(final @NonNull V viewer);

    /**
     * Removes a viewer from the boss bar.
     *
     * @param viewer a viewer
     */
    void removeViewer(final @NonNull V viewer);

    /**
     * Gets whether the boss bar has no viewers.
     *
     * @return if the boss bar is empty
     */
    boolean isEmpty();

    /**
     * Removes all viewers.
     */
    @Override
    void close();
  }

  /**
   * A facet that listens to boss bar changes, using packets.
   *
   * @param <V> a viewer type
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
     */
    default int createColor(final net.kyori.adventure.bossbar.BossBar.@NonNull Color color) {
      if(color == net.kyori.adventure.bossbar.BossBar.Color.PURPLE) {
        return 5;
      } else if(color == net.kyori.adventure.bossbar.BossBar.Color.PINK) {
        return 0;
      } else if(color == net.kyori.adventure.bossbar.BossBar.Color.BLUE) {
        return 1;
      } else if(color == net.kyori.adventure.bossbar.BossBar.Color.RED) {
        return 2;
      } else if(color == net.kyori.adventure.bossbar.BossBar.Color.GREEN) {
        return 3;
      } else if(color == net.kyori.adventure.bossbar.BossBar.Color.YELLOW) {
        return 4;
      } else if(color == net.kyori.adventure.bossbar.BossBar.Color.WHITE) {
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
     */
    default int createOverlay(final net.kyori.adventure.bossbar.BossBar.@NonNull Overlay overlay) {
      if(overlay == net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS) {
        return 0;
      } else if(overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_6) {
        return 1;
      } else if(overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_10) {
        return 2;
      } else if(overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_12) {
        return 3;
      } else if(overlay == net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_20) {
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
     */
    default byte createFlag(final byte flagBit, final @NonNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsAdded, final @NonNull Set<net.kyori.adventure.bossbar.BossBar.Flag> flagsRemoved) {
      byte bit = flagBit;
      for(final net.kyori.adventure.bossbar.BossBar.@NonNull Flag flag : flagsAdded) {
        if(flag == net.kyori.adventure.bossbar.BossBar.Flag.DARKEN_SCREEN) {
          bit |= 1;
        } else if(flag == net.kyori.adventure.bossbar.BossBar.Flag.PLAY_BOSS_MUSIC) {
          bit |= 1 << 1;
        } else if(flag == net.kyori.adventure.bossbar.BossBar.Flag.CREATE_WORLD_FOG) {
          bit |= 1 << 2;
        } else {
          logUnsupported(this, flag);
        }
      }
      for(final net.kyori.adventure.bossbar.BossBar.@NonNull Flag flag : flagsRemoved) {
        if(flag == net.kyori.adventure.bossbar.BossBar.Flag.DARKEN_SCREEN) {
          bit &= ~1;
        } else if(flag == net.kyori.adventure.bossbar.BossBar.Flag.PLAY_BOSS_MUSIC) {
          bit &= ~(1 << 1);
        } else if(flag == net.kyori.adventure.bossbar.BossBar.Flag.CREATE_WORLD_FOG) {
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
   */
  interface BossBarEntity<V, P> extends BossBar<V>, FakeEntity<V, P> {
    int OFFSET_PITCH = 30;
    int OFFSET_YAW = 0;
    int OFFSET_MAGNITUDE = 40;
    int INVULNERABLE_KEY = 20;
    int INVULNERABLE_TICKS = 890;

    @Override
    default void bossBarPercentChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final float oldPercent, final float newPercent) {
      this.health(newPercent);
    }

    @Override
    default void bossBarNameChanged(final net.kyori.adventure.bossbar.@NonNull BossBar bar, final @NonNull Component oldName, final @NonNull Component newName) {
      this.name(newName);
    }

    @Override
    default void addViewer(final @NonNull V viewer) {
      this.teleport(viewer, this.createPosition(viewer));
    }

    @Override
    default void removeViewer(final @NonNull V viewer) {
      this.teleport(viewer, null);
    }
  }

  /**
   * A facet for spawning client-side entities.
   *
   * @param <V> a viewer type
   * @param <P> a position type
   */
  interface FakeEntity<V, P> extends Position<V, P>, Closeable {
    /**
     * Teleports the entity for a viewer.
     *
     * @param viewer a viewer
     * @param position an entity position or {@code null} to remove
     */
    void teleport(final @NonNull V viewer, final @Nullable P position);

    /**
     * Sets the entity metadata.
     *
     * @param position a metadata position
     * @param data a value
     */
    void metadata(final int position, final @NonNull Object data);

    /**
     * Sets the entity visibility.
     *
     * @param invisible if invisible
     */
    void invisible(final boolean invisible);

    /**
     * Sets the entity health.
     *
     * @param health health level, between 0 and 1
     */
    void health(final float health);

    /**
     * Sets the entity name.
     *
     * @param name a name
     */
    void name(final @NonNull Component name);

    /**
     * Remove the entity for all viewers.
     */
    @Override
    void close();
  }
}
