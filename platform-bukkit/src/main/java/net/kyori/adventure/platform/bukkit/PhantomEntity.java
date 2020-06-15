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

  // TODOO: relative location?

  /**
   * Create a new instance of the provided entity type that can be spawned for specific players.
   *
   * @param spawnPos the location to spawn the entity at
   * @param type entity class
   * @param <T> entity type
   * @return a new instance. if on an unsupported platform, a no-op instance will be returned.
   */
  static <T extends Entity> PhantomEntity<T> of(final @NonNull Location spawnPos, final @NonNull Class<T> type) {
    if(!(LivingEntity.class.isAssignableFrom(type))) {
      throw new IllegalArgumentException("Only living entities can be spawned at the moment! Add new spawn packets to resolve this issue.");
    }
    if(Impl.SUPPORTED) {
      final T created = Impl.createFakeEntity(spawnPos, type);
      if(created != null) {
        return new Impl<>(created);
      }
    }
    return new NoOp<>();
  }

  /**
   * Get the entity being monitored
   *
   * @return The entity
   */
  T entity();

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

  class Impl<T extends Entity> implements PhantomEntity<T> {

    // Entity bits //
    private static final Class<? extends World> CLASS_CRAFT_WORLD = Crafty.findCraftClass("CraftWorld", World.class);
    private static final Class<?> CLASS_NMS_ENTITY = Crafty.findNmsClass("Entity");
    private static final Class<?> CLASS_NMS_LIVING_ENTITY = Crafty.findNmsClass("EntityLiving");
    private static final Class<?> CLASS_CRAFT_ENTITY = Crafty.findCraftClass("entity.CraftEntity");
    private static final Class<?> CLASS_DATA_WATCHER = Crafty.findNmsClass("DataWatcher");

    private static final MethodHandle CRAFT_WORLD_CREATE_ENTITY = Crafty.findMethod(CLASS_CRAFT_WORLD, "createEntity", CLASS_NMS_ENTITY, Location.class, Class.class);
    private static final MethodHandle CRAFT_ENTITY_GET_HANDLE = Crafty.findMethod(CLASS_CRAFT_ENTITY, "getHandle", CLASS_NMS_ENTITY);
    private static final MethodHandle NMS_ENTITY_GET_BUKKIT_ENTITY = Crafty.findMethod(CLASS_NMS_ENTITY, "getBukkitEntity", Entity.class);
    private static final MethodHandle NMS_ENTITY_GET_DATA_WATCHER = Crafty.findMethod(CLASS_NMS_ENTITY, "getDataWatcher", CLASS_DATA_WATCHER);

    // Packets //
    private static final Class<?> CLASS_SPAWN_LIVING_PACKET = Crafty.findNmsClass("PacketPlayOutSpawnEntityLiving");
    private static final MethodHandle NEW_SPAWN_LIVING_PACKET = Crafty.findConstructor(CLASS_SPAWN_LIVING_PACKET, CLASS_NMS_LIVING_ENTITY); // (entityToSpawn: LivingEntity)
    private static final Class<?> CLASS_ENTITY_DESTROY_PACKET = Crafty.findNmsClass("PacketPlayOutEntityDestroy");
    private static final MethodHandle NEW_ENTITY_DESTROY_PACKET = Crafty.findConstructor(CLASS_ENTITY_DESTROY_PACKET, int[].class); // (ids: int[])

    private static final Class<?> CLASS_ENTITY_METADATA_PACKET = Crafty.findNmsClass("PacketPlayOutEntityMetadata");
    private static final MethodHandle NEW_ENTITY_METADATA_PACKET = Crafty.findConstructor(CLASS_ENTITY_METADATA_PACKET, int.class, CLASS_DATA_WATCHER, boolean.class); // (entityId: int, DataWatcher, updateAll: boolean)

    static final boolean SUPPORTED = CRAFT_WORLD_CREATE_ENTITY != null && CRAFT_ENTITY_GET_HANDLE != null && NMS_ENTITY_GET_BUKKIT_ENTITY != null && NMS_ENTITY_GET_DATA_WATCHER != null;

    private final @NonNull T entity;
    private final Set<Player> watching = ConcurrentHashMap.newKeySet();

    Impl(@NonNull final T entity) {
      this.entity = entity;
    }

    private Object nmsEntity() {
      if(!CLASS_CRAFT_ENTITY.isInstance(this.entity)) return null;
      try {
        return CRAFT_ENTITY_GET_HANDLE.invoke(this.entity);
      } catch(Throwable throwable) {
        Knobs.logError("getting CraftBukkit entity for " + this.entity, throwable);
        return null;
      }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> T createFakeEntity(final Location pos, final Class<T> clazz) {
      if(!CLASS_CRAFT_WORLD.isInstance(pos.getWorld())) return null;

      try {
        final Object nmsEntity = CRAFT_WORLD_CREATE_ENTITY.invoke(pos.getWorld(), pos, clazz);
        return (T) NMS_ENTITY_GET_BUKKIT_ENTITY.invoke(nmsEntity);
      } catch(Throwable throwable) {
        Knobs.logError("creating fake entity for boss bar", throwable);
        return null;
      }
    }

    private Object createSpawnPacket() {
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
        return NEW_ENTITY_METADATA_PACKET.invoke(this.entity.getEntityId(), dataWatcher, true);
      } catch(Throwable throwable) {
        Knobs.logError("updating metadata for fake entity " + entity(), throwable);
        return null;
      }
    }

    @Override
    public T entity() {
      return this.entity;
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
    public boolean add(final @NonNull Player viewer) {
      if(this.watching.add(viewer)) {
        CraftBukkitHandlers.sendPacket(viewer, createSpawnPacket());
        return true;
      }
      return false;
    }

    @Override
    public boolean remove(final @NonNull Player viewer) {
      if(this.watching.remove(viewer)) {
        CraftBukkitHandlers.sendPacket(viewer, createDespawnPacket());
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
      }
    }

    @Override
    public void sendUpdate() {
      final Object updatePacket = createMetadataUpdatePacket();
      for(final Player ply : this.watching) {
        CraftBukkitHandlers.sendPacket(ply, updatePacket);
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
    public boolean watching(final @NonNull Player viewer) {
      return false;
    }

    @Override
    public boolean watching() {
      return false;
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
