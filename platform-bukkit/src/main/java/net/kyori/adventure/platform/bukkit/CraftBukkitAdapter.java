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

import com.google.gson.JsonDeserializer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.platform.bukkit.Crafty.nmsClass;

final class CraftBukkitAdapter implements Adapter {

  // Packets //
  private static final @Nullable MethodHandle CRAFT_PLAYER_GET_HANDLE;
  private static final @Nullable MethodHandle ENTITY_PLAYER_GET_CONNECTION;
  private static final @Nullable MethodHandle PLAYER_CONNECTION_SEND_PACKET;

  static {
    final @Nullable Class<?> craftPlayerClass = Crafty.findCraftClass("entity.CraftPlayer");
    final @Nullable Class<?> packetClass = Crafty.findNmsClass("Packet");
    @Nullable MethodHandle craftPlayerGetHandle = null;
    @Nullable MethodHandle entityPlayerGetConnection = null;
    @Nullable MethodHandle playerConnectionSendPacket = null;
    if(craftPlayerClass != null && packetClass != null) {
      try {
        final Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
        final Class<?> entityPlayerClass = getHandleMethod.getReturnType();
        craftPlayerGetHandle = Crafty.LOOKUP.unreflect(getHandleMethod);
        final Field playerConnectionField = entityPlayerClass.getField("playerConnection");
        entityPlayerGetConnection = Crafty.LOOKUP.unreflectGetter(playerConnectionField);
        final Class<?> playerConnectionClass = playerConnectionField.getType();
        playerConnectionSendPacket = Crafty.LOOKUP.findVirtual(playerConnectionClass, "sendPacket", methodType(void.class, packetClass));
      } catch(NoSuchMethodException | IllegalAccessException | NoSuchFieldException ignore) {
      }
    }
    CRAFT_PLAYER_GET_HANDLE = craftPlayerGetHandle;
    ENTITY_PLAYER_GET_CONNECTION = entityPlayerGetConnection;
    PLAYER_CONNECTION_SEND_PACKET = playerConnectionSendPacket;
  }

  // Titles //
  private static final @Nullable Class<?> CLASS_TITLE_PACKET = Crafty.findNmsClass("PacketPlayOutTitle");
  private static final @Nullable Class<?> CLASS_TITLE_ACTION = Crafty.findNmsClass("PacketPlayOutTitle$EnumTitleAction"); // welcome to spigot, where we can't name classes? i guess?
  private static final MethodHandle CONSTRUCTOR_TITLE_MESSAGE; // (EnumTitleAction, IChatBaseComponent)
  private static final @Nullable MethodHandle CONSTRUCTOR_TITLE_TIMES = Crafty.optionalConstructor(CLASS_TITLE_PACKET, methodType(int.class, int.class, int.class));
  private static final @Nullable Object TITLE_ACTION_TITLE = Crafty.enumValue(CLASS_TITLE_ACTION, "TITLE", 0);
  private static final @Nullable Object TITLE_ACTION_SUBTITLE = Crafty.enumValue(CLASS_TITLE_ACTION, "SUBTITLE", 1);
  private static final @Nullable Object TITLE_ACTION_ACTIONBAR = Crafty.enumValue(CLASS_TITLE_ACTION, "ACTIONBAR", 2);

  // Components //
  private static final @Nullable Class<?> CLASS_MESSAGE_TYPE = Crafty.findNmsClass("ChatMessageType");
  private static final @Nullable Object MESSAGE_TYPE_CHAT = Crafty.enumValue(CLASS_MESSAGE_TYPE, "CHAT", 0);
  private static final @Nullable Object MESSAGE_TYPE_SYSTEM = Crafty.enumValue(CLASS_MESSAGE_TYPE, "SYSTEM", 1);
  private static final @Nullable Object MESSAGE_TYPE_ACTIONBAR = Crafty.enumValue(CLASS_MESSAGE_TYPE, "GAME_INFO", 2);
  private static final UUID NIL_UUID = new UUID(0, 0);

  private static final @Nullable MethodHandle CHAT_PACKET_CONSTRUCTOR; // (ChatMessageType, IChatBaseComponent, UUID) -> PacketPlayOutChat
  private static final @Nullable MethodHandle BASE_COMPONENT_SERIALIZE; // (String) -> IChatBaseComponent

  private static final boolean VALID;

  static {
    MethodHandle chatPacketConstructor = null;
    MethodHandle serializeMethod = null;
    MethodHandle titlePacketConstructor = null;

    try {
      // Chat packet //
      final Class<?> baseComponentClass = Crafty.nmsClass("IChatBaseComponent");
      final Class<?> chatPacketClass = Crafty.nmsClass("PacketPlayOutChat");
      if(CLASS_TITLE_PACKET != null) {
        titlePacketConstructor = Crafty.LOOKUP.findConstructor(CLASS_TITLE_PACKET, methodType(void.class, CLASS_TITLE_ACTION, baseComponentClass));
      }
      // PacketPlayOutChat constructor changed for 1.16
      chatPacketConstructor = Crafty.optionalConstructor(chatPacketClass, methodType(void.class, baseComponentClass));
      if(chatPacketConstructor == null) {
        if (CLASS_MESSAGE_TYPE != null) {
          chatPacketConstructor = Crafty.LOOKUP.findConstructor(chatPacketClass, methodType(void.class, CLASS_MESSAGE_TYPE, baseComponentClass, UUID.class));
        }
      } else {
        // Create a function that ignores the message type and sender id arguments to call the underlying one-argument constructor
        chatPacketConstructor = dropArguments(dropArguments(chatPacketConstructor, 0, CLASS_MESSAGE_TYPE == null ? Object.class : CLASS_MESSAGE_TYPE), 2, UUID.class);
      }

      // Chat serializer //
      final Class<?> chatSerializerClass = Arrays.stream(baseComponentClass.getClasses())
        .filter(JsonDeserializer.class::isAssignableFrom)
        .findAny()
        // fallback to the 1.7 class?
        .orElseGet(() -> {
          return nmsClass("ChatSerializer");
        });
      final Method serialize = Arrays.stream(chatSerializerClass.getMethods())
        .filter(m -> Modifier.isStatic(m.getModifiers()))
        .filter(m -> m.getReturnType().equals(baseComponentClass))
        .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(String.class))
        .min(Comparator.comparing(Method::getName)) // prefer the #a method
        .orElse(null);

      if(serialize != null) {
        serializeMethod = Crafty.LOOKUP.unreflect(serialize);
      }
    } catch(NoSuchMethodException | IllegalAccessException | IllegalArgumentException e) {
    }
    CHAT_PACKET_CONSTRUCTOR = chatPacketConstructor;
    BASE_COMPONENT_SERIALIZE = serializeMethod;
    CONSTRUCTOR_TITLE_MESSAGE = titlePacketConstructor;
    VALID = serializeMethod != null;
  }

  @Override
  public void sendMessage(final List<? extends CommandSender> viewers, final Component component) {
    if(!VALID) {
      return;
    }
    send(viewers, component, msg -> createChatPacket(MESSAGE_TYPE_SYSTEM, msg, NIL_UUID));
  }

  @Override
  public void sendActionBar(final List<? extends CommandSender> viewers, final Component component) {
    if(!VALID) {
      return;
    }
    send(viewers, component, CraftBukkitAdapter::createActionBarPacket);
  }

  private static void send(final List<? extends CommandSender> viewers, final Component component, final Function<Component, Object> function) {
    Object packet = null;
    for(final Iterator<? extends CommandSender> iterator = viewers.iterator(); iterator.hasNext(); ) {
      final CommandSender sender = iterator.next();
      if(sender instanceof Player) {
        try {
          final Player player = (Player) sender;
          if(packet == null) {
            packet = function.apply(component);
            if(packet == null) {
              break; // failed, skip to next handler
            }
          }
          if(sendPacket(player, packet)) {
            iterator.remove();
          }
        } catch(final Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  private static boolean sendPacket(Player player, Object packet) {
    if(CRAFT_PLAYER_GET_HANDLE == null || ENTITY_PLAYER_GET_CONNECTION == null || PLAYER_CONNECTION_SEND_PACKET == null) {
      return false;
    }
    try {
      PLAYER_CONNECTION_SEND_PACKET.invoke(ENTITY_PLAYER_GET_CONNECTION.invoke(CRAFT_PLAYER_GET_HANDLE.invoke(player)), requireNonNull(packet, "packet"));
      return true;
    } catch(Throwable t) {
      return false;
    }
  }

  private static Object mcTextFromJson(String json) {
    if(BASE_COMPONENT_SERIALIZE == null) {
      throw new IllegalStateException("Not supported");
    }
    try {
      return BASE_COMPONENT_SERIALIZE.invoke(json);
    } catch(Throwable throwable) {
      return null;
    }
  }

  private static Object createChatPacket(Object chatType, Component message, UUID sender) {
    if(CHAT_PACKET_CONSTRUCTOR == null) {
      return null;
    }
    final Object nmsMessage = mcTextFromJson(GsonComponentSerializer.INSTANCE.serialize(message));
    if(nmsMessage == null) {
      return null;
    }

    try {
      return CHAT_PACKET_CONSTRUCTOR.invoke(chatType, nmsMessage, sender);
    } catch(Throwable throwable) {
      return null;
    }
  }

  private static Object createActionBarPacket(Component message) {
    if(CONSTRUCTOR_TITLE_MESSAGE == null) {
      return null;
    }
    final Object nmsMessage = mcTextFromJson(GsonComponentSerializer.INSTANCE.serialize(message));
    if(nmsMessage == null) {
      return null;
    }
    try {
      return CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_ACTIONBAR, nmsMessage);
    } catch(Throwable throwable) {
      return null;
    }
  }
}
