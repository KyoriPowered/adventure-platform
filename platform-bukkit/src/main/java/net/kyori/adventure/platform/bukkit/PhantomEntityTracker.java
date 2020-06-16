/*
 * This file is part of text-extras, licensed under the MIT License.
 *
 * Copyright (c) 2018 KyoriPowered
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
package net.kyori.adventure.platform.bukkit;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.platform.impl.Knobs;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

/**
 * A tracker for phantom entities that handles sending updates as appropriate.
 */
public class PhantomEntityTracker implements Listener {
  private final Plugin owner;
  private final Set<PhantomEntity<?>> trackedEntities = ConcurrentHashMap.newKeySet();
  private volatile boolean open = true;
  private final AtomicInteger relativeEntities = new AtomicInteger();
  private final AtomicBoolean isListeningToMove = new AtomicBoolean();
  private final Listener moveListener = new Listener() {};


  public PhantomEntityTracker(final Plugin owner) {
    this.owner = requireNonNull(owner, "owner");
    this.owner.getServer().getPluginManager().registerEvents(this, owner);
  }

  public <T extends Entity> PhantomEntity<T> create(final @NonNull Class<T> entity) {
    return create(new Location(this.owner.getServer().getWorlds().get(0), 0, 0, 0), entity);
  }

  public <T extends Entity> PhantomEntity<T> create(final @NonNull Location position, final @NonNull Class<T> entity) {
    if(!(LivingEntity.class.isAssignableFrom(entity))) {
      throw new IllegalArgumentException("Only living entities can be spawned at the moment! Add new spawn packets to resolve this issue.");
    }
    if(PhantomEntity.Impl.SUPPORTED) {
      final T created = PhantomEntity.Impl.createFakeEntity(position, entity);
      if(created != null) {
        return new PhantomEntity.Impl<>(this, created);
      }
    }
    return new PhantomEntity.NoOp<>();
  }

  /* package */ void onPlayerMove(final @NonNull PlayerMoveEvent event) {
    for(PhantomEntity<?> entity : this.trackedEntities) {
      entity.updateIfNecessary(event.getPlayer(), event.getTo().clone());
    }
  }

  @EventHandler
  public void onPlayerChangeWorld(final @NonNull PlayerChangedWorldEvent event) {
    respawnEntities(event.getPlayer());
  }

  @EventHandler
  public void onPlayerRespawn(final @NonNull PlayerRespawnEvent event) {
    // This event is called just before the player has respawned, let's add our entities on the next tick
    this.owner.getServer().getScheduler().scheduleSyncDelayedTask(this.owner, () -> respawnEntities(event.getPlayer()), 1);
  }

  private void respawnEntities(final @NonNull Player target) {
    for(PhantomEntity<?> entity : this.trackedEntities) {
      if(entity instanceof PhantomEntity.Impl<?> && entity.watching(target)) {
        ((PhantomEntity.Impl<?>) entity).sendSpawnPacket(target);
      }
    }

  }

  // TODO: respawn tracker, world change?

  public void close() {
    this.open = false;
    for(PhantomEntity<?> entity : this.trackedEntities) {
      entity.removeAll();
    }
    this.trackedEntities.clear();
  }

  /* package */ void updateTrackingState(final PhantomEntity<?> entity, final boolean wasRelative) {
    if(!this.open) {
      Knobs.logError("updating an entity on a closed tracker", new IllegalStateException());
    }

    if(this.trackedEntities.add(entity)) {
      if(entity.relative()) modifyRelative(true);
    } else {
      if(entity.relative() && !wasRelative) {
        modifyRelative(true);
      } else if(!entity.relative() && wasRelative) {
        modifyRelative(false);
      }
    }
  }

  /**
   * Modify the relative counter
   *
   * <p>We count relative entities so that we only listen to {@link #onPlayerMove(PlayerMoveEvent) move events}
   * when there are actual entities that ned to be updated.</p>
   *
   * @param add whether to add or remove a relative entity
   */
  private void modifyRelative(final boolean add) {
    int tracked;
    if(add) {
      tracked = this.relativeEntities.incrementAndGet();
    } else {
      tracked = this.relativeEntities.decrementAndGet();
    }
    if(tracked > 0 && this.isListeningToMove.compareAndSet(false, true)) {
      // register listener
      this.owner.getServer().getPluginManager().registerEvent(PlayerMoveEvent.class, this.moveListener, EventPriority.NORMAL, (listener, event) -> {
        this.onPlayerMove((PlayerMoveEvent) event);
      }, this.owner, true);
    } else if(tracked <= 0 && this.isListeningToMove.compareAndSet(true, false)) {
      // unregister
      PlayerMoveEvent.getHandlerList().unregister(this.moveListener);
    }
  }

  /* package */ void handleRemove(final PhantomEntity<?> entity) {
    this.trackedEntities.remove(entity);
  }
}
