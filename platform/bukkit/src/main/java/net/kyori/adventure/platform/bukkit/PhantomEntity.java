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

import java.lang.invoke.MethodHandle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.platform.impl.Knobs;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A simple tracker for clientside-only entities.
 *
 * <p>Managed entities will not be ticked normally, so all adjustments must be done manually.</p>
 */
interface PhantomEntity<T extends Entity> {

  /**
   * Get the entity being monitored
   *
   * @return The entity
   */
  T entity();

  /**
   * Get if this entity is in relative mode.
   *
   * @return relative status
   */
  boolean relative();

  /**
   * Set the entity's loctaion as an offset from the viewer.
   *
   * <p>A pitch and yaw offset of zero will place the entity {@code magnitudeOffset} in front of the player.</p>
   *
   * @param magnitudeOffset Magnitude of the offset
   * @param pitchOffset pitch offset from the viewer's position
   * @param yawOffset yaw offset from the viewer's position
   * @return this
   */
  PhantomEntity<T> relative(final double magnitudeOffset, final double pitchOffset, final double yawOffset);

  /**
   * Set the entity's location as a fixed position in the world.
   *
   * <p>The world field of the location is ignored.</p>
   *
   * @param position new position
   * @return this
   */
  PhantomEntity<T> location(final @NonNull Location position);

  /**
   * Get if the provided viewer is subscribed to our tracked entity
   *
   * @param viewer viewer to check
   * @return if the entity is spawned from the perspective of our viewer
   */
  boolean watching(final @NonNull Player viewer);

  /**
   * Return if any viewers are watching the tracked entity
   * @return the entity to track
   */
  boolean watching();

  /**
   * Set the invisibility flag on this entity.
   *
   * <p>This method will not send an update on its own -- use the {@link #sendUpdate()} method
   * when notification is desired.</p>
   *
   * @param invisible whether the entity should be invisible
   * @return this
   */
  PhantomEntity<T> invisible(final boolean invisible);

  /**
   * Get if this entity is invisible.
   *
   * @return invisibility state
   */
  boolean invisible();

  /**
   * Set the provided tracked data on the entity.
   *
   * <p>Data at the position must have already been added to the entity's data watcher
   * -- there is no internal validation that the tracked value instance exists.</p>
   *
   * @param position Data position
   * @param value Value. Untyped.
   * @return this
   */
  PhantomEntity<T> data(final int position, final Object value);

  /**
   * Spawn the entity for a viewer
   *
   * @param viewer viewer that should start tracking this entity
   * @return if the viewer was actually added
   */
  boolean add(final @NonNull Player viewer);

  /**
   * Despawn the entity for a viewer
   *
   * @param viewer viewer that the entity should be removed for.
   * @return if the viewer was actually removed
   */
  boolean remove(final @NonNull Player viewer);

  /**
   * Despawn this entity for all current viewers.
   */
  void removeAll();

  /**
   * Send a new update
   */
  void sendUpdate();

  default void updateIfNecessary(final @NonNull Player player, final @NonNull Location playerPos) {
  }

  /* package */ final class Impl<T extends Entity> implements PhantomEntity<T> {

    // Entity bits //
    private static final Class<? extends World> CLASS_CRAFT_WORLD = Crafty.findCraftClass("CraftWorld", World.class);
    private static final Class<?> CLASS_NMS_ENTITY = Crafty.findNmsClass("Entity");
    private static final Class<?> CLASS_NMS_LIVING_ENTITY = Crafty.findNmsClass("EntityLiving");
    private static final Class<?> CLASS_CRAFT_ENTITY = Crafty.findCraftClass("entity.CraftEntity");
    private static final Class<?> CLASS_DATA_WATCHER = Crafty.findNmsClass("DataWatcher");

    private static final MethodHandle CRAFT_WORLD_CREATE_ENTITY = Crafty.findMethod(CLASS_CRAFT_WORLD, "createEntity", CLASS_NMS_ENTITY, Location.class, Class.class);
    private static final MethodHandle CRAFT_ENTITY_GET_HANDLE = Crafty.findMethod(CLASS_CRAFT_ENTITY, "getHandle", CLASS_NMS_ENTITY);
    private static final MethodHandle NMS_ENTITY_GET_BUKKIT_ENTITY = Crafty.findMethod(CLASS_NMS_ENTITY, "getBukkitEntity", CLASS_CRAFT_ENTITY);
    private static final MethodHandle NMS_ENTITY_GET_DATA_WATCHER = Crafty.findMethod(CLASS_NMS_ENTITY, "getDataWatcher", CLASS_DATA_WATCHER);
    private static final MethodHandle NMS_ENTITY_SET_LOCATION = Crafty.findMethod(CLASS_NMS_ENTITY, "setLocation", void.class, double.class, double.class, double.class, float.class, float.class); // (x, y, z, pitch, yaw) -> void
    private static final MethodHandle NMS_ENTITY_IS_INVISIBLE = Crafty.findMethod(CLASS_NMS_ENTITY, "isInvisible", boolean.class);
    private static final MethodHandle NMS_ENTITY_SET_INVISIBLE = Crafty.findMethod(CLASS_NMS_ENTITY, "setInvisible", void.class, boolean.class);
    private static final MethodHandle DATA_WATCHER_WATCH = Crafty.findMethod(CLASS_DATA_WATCHER, "watch", void.class, int.class, Object.class);

    // Packets //
    private static final Class<?> CLASS_SPAWN_LIVING_PACKET = Crafty.findNmsClass("PacketPlayOutSpawnEntityLiving");
    private static final MethodHandle NEW_SPAWN_LIVING_PACKET = Crafty.findConstructor(CLASS_SPAWN_LIVING_PACKET, CLASS_NMS_LIVING_ENTITY); // (entityToSpawn: LivingEntity)
    private static final Class<?> CLASS_ENTITY_DESTROY_PACKET = Crafty.findNmsClass("PacketPlayOutEntityDestroy");
    private static final MethodHandle NEW_ENTITY_DESTROY_PACKET = Crafty.findConstructor(CLASS_ENTITY_DESTROY_PACKET, int[].class); // (ids: int[])
    private static final Class<?> CLASS_ENTITY_METADATA_PACKET = Crafty.findNmsClass("PacketPlayOutEntityMetadata");
    private static final MethodHandle NEW_ENTITY_METADATA_PACKET = Crafty.findConstructor(CLASS_ENTITY_METADATA_PACKET, int.class, CLASS_DATA_WATCHER, boolean.class); // (entityId: int, DataWatcher, updateAll: boolean)
    private static final Class<?> CLASS_ENTITY_TELEPORT_PACKET = Crafty.findNmsClass("PacketPlayOutEntityTeleport");
    private static final MethodHandle NEW_ENTITY_TELEPORT_PACKET = Crafty.findConstructor(CLASS_ENTITY_TELEPORT_PACKET, CLASS_NMS_ENTITY);

    static final boolean SUPPORTED = CRAFT_WORLD_CREATE_ENTITY != null && CRAFT_ENTITY_GET_HANDLE != null && NMS_ENTITY_GET_BUKKIT_ENTITY != null && NMS_ENTITY_GET_DATA_WATCHER != null;

    private final @NonNull PhantomEntityTracker tracker;
    private final @NonNull T entity;
    private final Set<Player> watching = ConcurrentHashMap.newKeySet();
    private volatile double relativeOffsetDistance;
    private volatile double relativeOffsetPitch;
    private volatile double relativeOffsetYaw;
    private volatile boolean locationDirty;

    Impl(final @NonNull PhantomEntityTracker tracker, final @NonNull T entity) {
      this.tracker = tracker;
      this.entity = entity;
    }

    Object nmsEntity() {
      if(!CLASS_CRAFT_ENTITY.isInstance(this.entity)) return null;
      try {
        return CRAFT_ENTITY_GET_HANDLE.invoke(this.entity);
      } catch(Throwable throwable) {
        Knobs.logError("getting CraftBukkit entity for " + this.entity, throwable);
        return null;
      }
    }

    @SuppressWarnings("unchecked")
    static <T extends Entity> T createFakeEntity(final Location pos, final Class<T> clazz) {
      if(!CLASS_CRAFT_WORLD.isInstance(pos.getWorld())) return null;

      try {
        final Object nmsEntity = CRAFT_WORLD_CREATE_ENTITY.invoke(pos.getWorld(), pos, clazz);
        return (T) NMS_ENTITY_GET_BUKKIT_ENTITY.invoke(nmsEntity);
      } catch(Throwable throwable) {
        Knobs.logError("creating fake entity for boss bar", throwable);
        return null;
      }
    }

    /* package */ Object createSpawnPacket() {
      // Later versions of MC add a createSpawnPacket()Packet method on Entity -- for broader support that could be used.
      // For 1.8 at least, we are stuck with this.
      if(entity() instanceof LivingEntity) {
        final Object mcEntity = this.nmsEntity();
        if(mcEntity != null) {
          try {
            return NEW_SPAWN_LIVING_PACKET.invoke(mcEntity);
          } catch(Throwable throwable) {
            Knobs.logError("creating spawn packet for fake entity " + entity(), throwable);
          }
        }
      }
      return null;
    }

    private Object createDespawnPacket() {
      try {
        return NEW_ENTITY_DESTROY_PACKET.invoke(entity().getEntityId());
      } catch(Throwable throwable) {
        Knobs.logError("creating despawn packet for fake entity " + entity(), throwable);
        return null;
      }
    }

    private Object createMetadataUpdatePacket() {
      try {
        final Object nmsEntity = nmsEntity();
        if(nmsEntity == null) return null;

        final Object dataWatcher = NMS_ENTITY_GET_DATA_WATCHER.invoke(nmsEntity);
        return NEW_ENTITY_METADATA_PACKET.invoke(this.entity.getEntityId(), dataWatcher, false);
      } catch(Throwable throwable) {
        Knobs.logError("updating metadata for fake entity " + entity(), throwable);
        return null;
      }
    }

    private Object createLocationUpdatePacket() {
      try {
        final Object nmsEntity = nmsEntity();
        if(nmsEntity == null) return null;

        return NEW_ENTITY_TELEPORT_PACKET.invoke(nmsEntity);
      } catch(Throwable throwable) {
        Knobs.logError("creating location update packet", throwable);
        return null;
      }
    }

    @Override
    public T entity() {
      return this.entity;
    }

    @Override
    public boolean relative() {
      return this.relativeOffsetDistance != 0 || this.relativeOffsetPitch != 0 || this.relativeOffsetYaw != 0;
    }

    @Override
    public PhantomEntity<T> relative(final double magnitudeOffset, final double pitchOffset, final double yawOffset) {
      final boolean wasRelative = relative();
      this.relativeOffsetDistance = magnitudeOffset;
      this.relativeOffsetPitch = pitchOffset;
      this.relativeOffsetYaw = yawOffset;
      this.tracker.updateTrackingState(this, wasRelative);
      return this;
    }

    @Override
    public PhantomEntity<T> location(final @NonNull Location position) {
      final boolean wasRelative = relative();
      this.relativeOffsetDistance = 0;
      this.relativeOffsetPitch = 0;
      this.relativeOffsetYaw = 0;
      this.tracker.updateTrackingState(this, wasRelative);
      location0(position);
      this.locationDirty = true;
      return this;
    }

    private void location0(final @NonNull Location position) {
      try {
        NMS_ENTITY_SET_LOCATION.invoke(nmsEntity(), position.getX(), position.getY(), position.getZ(), position.getPitch(), position.getYaw());
      } catch(Throwable throwable) {
        Knobs.logError("setting position for phantom entity " + this.entity, throwable);
      }
    }

    @Override
    public boolean watching(final @NonNull Player viewer) {
      return this.watching.contains(viewer);
    }

    @Override
    public boolean watching() {
      return !this.watching.isEmpty();
    }

    @Override
    public PhantomEntity<T> invisible(final boolean invisible) {
      if(NMS_ENTITY_SET_INVISIBLE != null) {
        try {
          NMS_ENTITY_SET_INVISIBLE.invoke(nmsEntity(), invisible);
        } catch(Throwable thr) {
          Knobs.logError("setting invisibility for entity", thr);
        }
      }
      return this;
    }

    @Override
    public boolean invisible() {
      if(NMS_ENTITY_IS_INVISIBLE != null) {
        try {
          return (boolean)NMS_ENTITY_IS_INVISIBLE.invoke(nmsEntity());
        } catch(Throwable thr) {
          Knobs.logError("getting invisibility for entity", thr);
        }
      }
      return false;
    }

    public PhantomEntity<T> data(final int position, final Object value) {
      // DataWatchers were refactored at some point and use TrackedData as their key, not ints -- but this works for 1.8
      if(DATA_WATCHER_WATCH != null) {
        try {
          final Object dataWatcher = NMS_ENTITY_GET_DATA_WATCHER.invoke(nmsEntity());
          DATA_WATCHER_WATCH.invoke(dataWatcher, position, value);
        } catch(Throwable throwable) {
          Knobs.logError("watching data", throwable);
        }
      }
      return this;
    }

    @Override
    public boolean add(final @NonNull Player viewer) {
      if(this.watching.add(viewer)) {
        sendSpawnPacket(viewer);
        this.tracker.updateTrackingState(this, relative());
        return true;
      }
      return false;
    }

    /* package */ void sendSpawnPacket(final @NonNull Player viewer) {
      if(relative()) {
        this.location0(makeRelative(viewer.getLocation()));
      }
      CraftBukkitHandlers.sendPacket(viewer, createSpawnPacket());
    }

    @Override
    public boolean remove(final @NonNull Player viewer) {
      if(this.watching.remove(viewer)) {
        CraftBukkitHandlers.sendPacket(viewer, createDespawnPacket());
        if(this.watching.isEmpty()) {
          this.tracker.handleRemove(this);
        }
        return true;
      }
      return false;
    }

    @Override
    public void removeAll() {
      if(!this.watching.isEmpty()) {
        final Object despawnPacket = createDespawnPacket();
        for(final Player viewer : this.watching) {
          CraftBukkitHandlers.sendPacket(viewer, despawnPacket);
        }
        this.watching.clear();
        this.tracker.handleRemove(this);
      }
    }

    @Override
    public void sendUpdate() {
      final Object metadataPacket = createMetadataUpdatePacket();
      final Object locationPacket = this.locationDirty ? createLocationUpdatePacket() : null;
      for(final Player ply : this.watching) {
        CraftBukkitHandlers.sendPacket(ply, metadataPacket);
        CraftBukkitHandlers.sendPacket(ply, locationPacket);
      }
      this.locationDirty = false;
    }

    private Location makeRelative(final @NonNull Location pos) {
      pos.setPitch(pos.getPitch() - (float) this.relativeOffsetPitch);
      pos.setYaw(pos.getYaw() + (float) this.relativeOffsetYaw);
      if(this.relativeOffsetDistance != 0) {
        pos.add(pos.getDirection().multiply(this.relativeOffsetDistance));
      }
      return pos;
    }

    @Override
    public void updateIfNecessary(final @NonNull Player player, final @NonNull Location playerPos) {
      if(relative()
        && this.watching.contains(player)) {
        final Location pos = makeRelative(playerPos);
        location0(pos);
        CraftBukkitHandlers.sendPacket(player, createLocationUpdatePacket());
      }
    }
  }


  /**
   * Fallback handler for unsupported platforms
   *
   * @param <T> entity type
   */
  class NoOp<T extends Entity> implements PhantomEntity<T> {

    @Override
    public T entity() {
      return null;
    }

    @Override
    public boolean relative() {
      return false;
    }

    @Override
    public PhantomEntity<T> relative(final double magnitudeOffset, final double pitchOffset, final double yawOffset) {
      return this;
    }

    @Override
    public PhantomEntity<T> location(final @NonNull Location position) {
      return this;
    }

    @Override
    public boolean watching(final @NonNull Player viewer) {
      return false;
    }

    @Override
    public boolean watching() {
      return false;
    }

    @Override
    public PhantomEntity<T> invisible(final boolean invisible) {
      return this;
    }

    @Override
    public boolean invisible() {
      return false;
    }

    @Override
    public PhantomEntity<T> data(final int position, final Object value) {
      return this;
    }

    @Override
    public boolean add(final @NonNull Player viewer) {
      return false;
    }

    @Override
    public boolean remove(final @NonNull Player viewer) {
      return false;
    }

    @Override
    public void removeAll() {
    }

    @Override
    public void sendUpdate() {
    }
  }
}
