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
package net.kyori.adventure.platform.bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findConstructor;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findField;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findMcClassName;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findNmsClassName;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.lookup;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.searchMethod;
import static net.kyori.adventure.platform.facet.Knob.logError;

final class CraftBukkitAccess {
  static final @Nullable Class<?> CLASS_CHAT_COMPONENT = findClass(
    findNmsClassName("IChatBaseComponent"),
    findMcClassName("network.chat.IChatBaseComponent"),
    findMcClassName("network.chat.Component")
  );
  static final @Nullable Class<?> CLASS_REGISTRY = findClass(
    findNmsClassName("IRegistry"),
    findMcClassName("core.IRegistry"),
    findMcClassName("core.Registry")
  );
  static final @Nullable Class<?> CLASS_SERVER_LEVEL = findClass(
    findMcClassName("server.level.WorldServer"),
    findMcClassName("server.level.ServerLevel")
  );
  static final @Nullable Class<?> CLASS_LEVEL = findClass(
    findMcClassName("world.level.World"),
    findMcClassName("world.level.Level")
  );
  static final @Nullable Class<?> CLASS_REGISTRY_ACCESS = findClass(
    findMcClassName("core.IRegistryCustom"),
    findMcClassName("core.RegistryAccess")
  );
  static final @Nullable Class<?> CLASS_RESOURCE_KEY = findClass(findMcClassName("resources.ResourceKey"));
  static final @Nullable Class<?> CLASS_RESOURCE_LOCATION = findClass(
    findNmsClassName("MinecraftKey"),
    findMcClassName("resources.MinecraftKey"),
    findMcClassName("resources.ResourceLocation")
  );
  static final @Nullable Class<?> CLASS_NMS_ENTITY = findClass(
    findNmsClassName("Entity"),
    findMcClassName("world.entity.Entity")
  );
  static final @Nullable Class<?> CLASS_BUILT_IN_REGISTRIES = findClass(findMcClassName("core.registries.BuiltInRegistries"));
  static final @Nullable Class<?> CLASS_HOLDER = findClass(findMcClassName("core.Holder"));
  static final @Nullable Class<?> CLASS_WRITABLE_REGISTRY = findClass(
    findNmsClassName("IRegistryWritable"),
    findMcClassName("core.IRegistryWritable"),
    findMcClassName("core.WritableRegistry")
  );

  private CraftBukkitAccess() {
  }

  static final class Chat1_19_3 {
    static final @Nullable MethodHandle NEW_RESOURCE_LOCATION = findConstructor(CLASS_RESOURCE_LOCATION, String.class, String.class);
    static final @Nullable MethodHandle RESOURCE_KEY_CREATE = searchMethod(CLASS_RESOURCE_KEY, Modifier.PUBLIC | Modifier.STATIC, "create", CLASS_RESOURCE_KEY, CLASS_RESOURCE_KEY, CLASS_RESOURCE_LOCATION);
    static final @Nullable MethodHandle SERVER_PLAYER_GET_LEVEL = searchMethod(CraftBukkitFacet.CRAFT_PLAYER_GET_HANDLE.type().returnType(), Modifier.PUBLIC, "getLevel", CLASS_SERVER_LEVEL);
    static final @Nullable MethodHandle SERVER_LEVEL_GET_REGISTRY_ACCESS = searchMethod(CLASS_SERVER_LEVEL, Modifier.PUBLIC, "registryAccess", CLASS_REGISTRY_ACCESS);
    static final @Nullable MethodHandle LEVEL_GET_REGISTRY_ACCESS = searchMethod(CLASS_LEVEL, Modifier.PUBLIC, "registryAccess", CLASS_REGISTRY_ACCESS);
    static final @Nullable MethodHandle ACTUAL_GET_REGISTRY_ACCESS = SERVER_LEVEL_GET_REGISTRY_ACCESS == null ? LEVEL_GET_REGISTRY_ACCESS : SERVER_LEVEL_GET_REGISTRY_ACCESS;
    static final @Nullable MethodHandle REGISTRY_ACCESS_GET_REGISTRY_OPTIONAL = searchMethod(CLASS_REGISTRY_ACCESS, Modifier.PUBLIC, "registry", Optional.class, CLASS_RESOURCE_KEY);
    static final @Nullable MethodHandle REGISTRY_GET_OPTIONAL = searchMethod(CLASS_REGISTRY, Modifier.PUBLIC, "getOptional", Optional.class, CLASS_RESOURCE_LOCATION);
    static final @Nullable MethodHandle REGISTRY_GET_ID = searchMethod(CLASS_REGISTRY, Modifier.PUBLIC, "getId", int.class, Object.class);
    static final @Nullable MethodHandle DISGUISED_CHAT_PACKET_CONSTRUCTOR;
    static final @Nullable MethodHandle CHAT_TYPE_BOUND_NETWORK_CONSTRUCTOR;

    static final Object CHAT_TYPE_RESOURCE_KEY;

    static {
      MethodHandle boundNetworkConstructor = null;
      MethodHandle disguisedChatPacketConstructor = null;
      Object chatTypeResourceKey = null;

      try {
        Class<?> classChatTypeBoundNetwork = findClass(findMcClassName("network.chat.ChatType$BoundNetwork"));
        if (classChatTypeBoundNetwork == null) {
          final Class<?> parentClass = findClass(findMcClassName("network.chat.ChatMessageType"));
          if (parentClass != null) {
            for (final Class<?> childClass : parentClass.getClasses()) {
              boundNetworkConstructor = findConstructor(childClass, int.class, CLASS_CHAT_COMPONENT, CLASS_CHAT_COMPONENT);
              if (boundNetworkConstructor != null) {
                classChatTypeBoundNetwork = childClass;
                break;
              }
            }
          }
        }

        final Class<?> disguisedChatPacketClass = findClass(findMcClassName("network.protocol.game.ClientboundDisguisedChatPacket"));
        if (disguisedChatPacketClass != null && classChatTypeBoundNetwork != null) {
          disguisedChatPacketConstructor = findConstructor(disguisedChatPacketClass, CLASS_CHAT_COMPONENT, classChatTypeBoundNetwork);
        }

        if (NEW_RESOURCE_LOCATION != null && RESOURCE_KEY_CREATE != null) {
          final MethodHandle createRegistryKey = searchMethod(CLASS_RESOURCE_KEY, Modifier.PUBLIC | Modifier.STATIC, "createRegistryKey", CLASS_RESOURCE_KEY, CLASS_RESOURCE_LOCATION);
          if (createRegistryKey != null) {
            chatTypeResourceKey = createRegistryKey.invoke(NEW_RESOURCE_LOCATION.invoke("minecraft", "chat_type"));
          }
        }
      } catch (final Throwable error) {
        logError(error, "Failed to initialize 1.19.3 chat support");
      }

      DISGUISED_CHAT_PACKET_CONSTRUCTOR = disguisedChatPacketConstructor;
      CHAT_TYPE_BOUND_NETWORK_CONSTRUCTOR = boundNetworkConstructor;
      CHAT_TYPE_RESOURCE_KEY = chatTypeResourceKey;
    }

    private Chat1_19_3() {
    }

    static boolean isSupported() {
      return ACTUAL_GET_REGISTRY_ACCESS != null && REGISTRY_ACCESS_GET_REGISTRY_OPTIONAL != null && REGISTRY_GET_OPTIONAL != null && CHAT_TYPE_BOUND_NETWORK_CONSTRUCTOR != null && DISGUISED_CHAT_PACKET_CONSTRUCTOR != null && CHAT_TYPE_RESOURCE_KEY != null;
    }
  }

  static final class EntitySound {
    static final @Nullable Class<?> CLASS_CLIENTBOUND_ENTITY_SOUND = findClass(
      findNmsClassName("PacketPlayOutEntitySound"),
      findMcClassName("network.protocol.game.PacketPlayOutEntitySound"),
      findMcClassName("network.protocol.game.ClientboundSoundEntityPacket")
    );
    static final @Nullable Class<?> CLASS_SOUND_SOURCE = findClass(
      findNmsClassName("SoundCategory"),
      findMcClassName("sounds.SoundCategory"),
      findMcClassName("sounds.SoundSource")
    );
    static final @Nullable Class<?> CLASS_SOUND_EVENT = findClass(
      findNmsClassName("SoundEffect"),
      findMcClassName("sounds.SoundEffect"),
      findMcClassName("sounds.SoundEvent")
    );

    static final @Nullable MethodHandle SOUND_SOURCE_GET_NAME;

    static {
      MethodHandle soundSourceGetName = null;
      if (CLASS_SOUND_SOURCE != null) {
        for (final Method method : CLASS_SOUND_SOURCE.getDeclaredMethods()) {
          if (method.getReturnType().equals(String.class)
            && method.getParameterCount() == 0
            && !"name".equals(method.getName())
            && Modifier.isPublic(method.getModifiers())
          ) {
            try {
              soundSourceGetName = lookup().unreflect(method);
            } catch (final IllegalAccessException ex) {
              // ignored, getName is public
            }
            break;
          }
        }
      }
      SOUND_SOURCE_GET_NAME = soundSourceGetName;
    }

    private EntitySound() {
    }

    static boolean isSupported() {
      return SOUND_SOURCE_GET_NAME != null;
    }
  }

  static final class EntitySound_1_19_3 {

    static final @Nullable MethodHandle NEW_RESOURCE_LOCATION = findConstructor(CLASS_RESOURCE_LOCATION, String.class, String.class);
    static final @Nullable MethodHandle REGISTRY_GET_OPTIONAL = searchMethod(CLASS_REGISTRY, Modifier.PUBLIC, "getOptional", Optional.class, CLASS_RESOURCE_LOCATION);
    static final @Nullable MethodHandle REGISTRY_WRAP_AS_HOLDER = searchMethod(CLASS_REGISTRY, Modifier.PUBLIC, "wrapAsHolder", CLASS_HOLDER, Object.class);
    static final @Nullable MethodHandle SOUND_EVENT_CREATE_VARIABLE_RANGE = searchMethod(EntitySound.CLASS_SOUND_EVENT, Modifier.PUBLIC | Modifier.STATIC, "createVariableRangeEvent", EntitySound.CLASS_SOUND_EVENT, CLASS_RESOURCE_LOCATION);
    static final @Nullable MethodHandle NEW_CLIENTBOUND_ENTITY_SOUND = findConstructor(EntitySound.CLASS_CLIENTBOUND_ENTITY_SOUND, CLASS_HOLDER, EntitySound.CLASS_SOUND_SOURCE, CLASS_NMS_ENTITY, float.class, float.class, long.class);

    static final @Nullable Object SOUND_EVENT_REGISTRY;

    static {
      Object soundEventRegistry = null;
      try {
        final Field soundEventRegistryField = findField(CLASS_BUILT_IN_REGISTRIES, CLASS_REGISTRY, "SOUND_EVENT");
        if (soundEventRegistryField != null) {
          soundEventRegistry = soundEventRegistryField.get(null);
        } else if (CLASS_BUILT_IN_REGISTRIES != null && REGISTRY_GET_OPTIONAL != null && NEW_RESOURCE_LOCATION != null) {
          Object rootRegistry = null;
          for (final Field field : CLASS_BUILT_IN_REGISTRIES.getDeclaredFields()) {
            final int mask = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;
            if ((field.getModifiers() & mask) == mask
              && field.getType().equals(CLASS_WRITABLE_REGISTRY)) {
              field.setAccessible(true);
              rootRegistry = field.get(null);
              break;
            }
          }
          if (rootRegistry != null) {
            soundEventRegistry = ((Optional<?>) REGISTRY_GET_OPTIONAL.invoke(rootRegistry, NEW_RESOURCE_LOCATION.invoke("minecraft", "sound_event"))).orElse(null);
          }
        }
      } catch (final Throwable error) {
        logError(error, "Failed to initialize EntitySound_1_19_3 CraftBukkit facet");
      }
      SOUND_EVENT_REGISTRY = soundEventRegistry;
    }

    private EntitySound_1_19_3() {
    }

    static boolean isSupported() {
      return NEW_CLIENTBOUND_ENTITY_SOUND != null && SOUND_EVENT_REGISTRY != null && NEW_RESOURCE_LOCATION != null && REGISTRY_GET_OPTIONAL != null && REGISTRY_WRAP_AS_HOLDER != null && SOUND_EVENT_CREATE_VARIABLE_RANGE != null;
    }
  }
}
