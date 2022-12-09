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
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findClass;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findConstructor;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findMcClassName;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.findNmsClassName;
import static net.kyori.adventure.platform.bukkit.MinecraftReflection.searchMethod;
import static net.kyori.adventure.platform.facet.Knob.logError;

final class CraftBukkitAccess {
  static final @Nullable Class<?> CLASS_CHAT_COMPONENT = findClass(
    findNmsClassName("IChatBaseComponent"),
    findMcClassName("network.chat.IChatBaseComponent"),
    findMcClassName("network.chat.Component")
  );
  static final @Nullable Class<?> CLASS_REGISTRY = findClass(
    findMcClassName("core.IRegistry"),
    findMcClassName("core.Registry")
  );
  static final @Nullable Class<?> CLASS_SERVER_LEVEL = findClass(
    findMcClassName("server.level.WorldServer"),
    findMcClassName("server.level.ServerLevel")
  );
  static final @Nullable Class<?> CLASS_REGISTRY_ACCESS = findClass(
    findMcClassName("core.IRegistryCustom"),
    findMcClassName("core.RegistryAccess")
  );
  static final @Nullable Class<?> CLASS_RESOURCE_KEY = findClass(findMcClassName("resources.ResourceKey"));
  static final @Nullable Class<?> CLASS_RESOURCE_LOCATION = findClass(
    findMcClassName("resources.MinecraftKey"),
    findMcClassName("resources.ResourceLocation")
  );

  private CraftBukkitAccess() {
  }

  static final class Chat1_19_3 {
    static final @Nullable MethodHandle NEW_RESOURCE_LOCATION = findConstructor(CLASS_RESOURCE_LOCATION, String.class, String.class);
    static final @Nullable MethodHandle RESOURCE_KEY_CREATE = searchMethod(CLASS_RESOURCE_KEY, Modifier.PUBLIC | Modifier.STATIC, "create", CLASS_RESOURCE_KEY, CLASS_RESOURCE_KEY, CLASS_RESOURCE_LOCATION);
    static final @Nullable MethodHandle SERVER_PLAYER_GET_LEVEL = searchMethod(CraftBukkitFacet.CRAFT_PLAYER_GET_HANDLE.type().returnType(), Modifier.PUBLIC, "getLevel", CLASS_SERVER_LEVEL);
    static final @Nullable MethodHandle SERVER_LEVEL_GET_REGISTRY_ACCESS = searchMethod(CLASS_SERVER_LEVEL, Modifier.PUBLIC, "registryAccess", CLASS_REGISTRY_ACCESS);
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
      return SERVER_LEVEL_GET_REGISTRY_ACCESS != null && REGISTRY_ACCESS_GET_REGISTRY_OPTIONAL != null && REGISTRY_GET_OPTIONAL != null && CHAT_TYPE_BOUND_NETWORK_CONSTRUCTOR != null && DISGUISED_CHAT_PACKET_CONSTRUCTOR != null && CHAT_TYPE_RESOURCE_KEY != null;
    }
  }
}
