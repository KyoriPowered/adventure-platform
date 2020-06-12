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

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.Knobs;
import net.kyori.adventure.platform.impl.TypedHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodType.methodType;
import static net.kyori.adventure.platform.bukkit.Crafty.nmsClass;
import static net.kyori.adventure.platform.bukkit.Crafty.optionalConstructor;

public class CraftBukkitHandlers {
  
  private static final boolean ENABLED = Knobs.enabled("craftbukkit");
  
  private static final @Nullable Class<? extends Player> CLASS_CRAFT_PLAYER = Crafty.findCraftClass("entity.CraftPlayer", Player.class);

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
      } catch(NoSuchMethodException | IllegalAccessException | NoSuchFieldException ex) {
        Knobs.logError("finding packet send methods", ex);
      }
    }
    CRAFT_PLAYER_GET_HANDLE = craftPlayerGetHandle;
    ENTITY_PLAYER_GET_CONNECTION = entityPlayerGetConnection;
    PLAYER_CONNECTION_SEND_PACKET = playerConnectionSendPacket;
  }

  /* package */ static class PacketSendingHandler<V extends CommandSender> extends TypedHandler<V> {

    @SuppressWarnings("unchecked")
    protected PacketSendingHandler() {
      super((Class<V>) CLASS_CRAFT_PLAYER);
    }

    @Override
    public boolean isAvailable() {
      return ENABLED && super.isAvailable() && CRAFT_PLAYER_GET_HANDLE != null && ENTITY_PLAYER_GET_CONNECTION != null && PLAYER_CONNECTION_SEND_PACKET != null;
    }

    public void send(final @NonNull V player, final @Nullable Object packet) {
      if(packet == null) {
        return;
      }

      try {
        PLAYER_CONNECTION_SEND_PACKET.invoke(ENTITY_PLAYER_GET_CONNECTION.invoke(CRAFT_PLAYER_GET_HANDLE.invoke(player)));
      } catch(Throwable throwable) {
        Knobs.logError("sending packet to user", throwable);
      }
    }
  }

  // Components //
  private static final @Nullable Class<?> CLASS_CHAT_COMPONENT = Crafty.findNmsClass("IChatBaseComponent");
  private static final @Nullable Class<?> CLASS_MESSAGE_TYPE = Crafty.findNmsClass("ChatMessageType");
  private static final @Nullable Object MESSAGE_TYPE_CHAT = Crafty.enumValue(CLASS_MESSAGE_TYPE, "CHAT", 0);
  private static final @Nullable Object MESSAGE_TYPE_SYSTEM = Crafty.enumValue(CLASS_MESSAGE_TYPE, "SYSTEM", 1);
  private static final @Nullable Object MESSAGE_TYPE_ACTIONBAR = Crafty.enumValue(CLASS_MESSAGE_TYPE, "GAME_INFO", 2);
  private static final Gson MC_TEXT_GSON;

  private static final @Nullable MethodHandle LEGACY_CHAT_PACKET_CONSTRUCTOR; // (IChatBaseComponent, byte)
  private static final @Nullable MethodHandle CHAT_PACKET_CONSTRUCTOR; // (ChatMessageType, IChatBaseComponent, UUID) -> PacketPlayOutChat

  static {
    MethodHandle legacyChatPacketConstructor = null;
    MethodHandle chatPacketConstructor = null;
    Gson gson = null;

    try {
      if(CLASS_CHAT_COMPONENT != null) {
        // Chat packet //
        final Class<?> chatPacketClass = Crafty.nmsClass("PacketPlayOutChat");
        // PacketPlayOutChat constructor changed for 1.16
        chatPacketConstructor = Crafty.optionalConstructor(chatPacketClass, methodType(void.class, CLASS_CHAT_COMPONENT));
        if(chatPacketConstructor == null) {
          if(CLASS_MESSAGE_TYPE != null) {
            chatPacketConstructor = Crafty.LOOKUP.findConstructor(chatPacketClass, methodType(void.class, CLASS_MESSAGE_TYPE, CLASS_CHAT_COMPONENT, UUID.class));
          }
        } else {
          // Create a function that ignores the message type and sender id arguments to call the underlying one-argument constructor
          chatPacketConstructor = dropArguments(chatPacketConstructor, 1, CLASS_MESSAGE_TYPE == null ? Object.class : CLASS_MESSAGE_TYPE, UUID.class);
        }
        legacyChatPacketConstructor = optionalConstructor(chatPacketClass, methodType(void.class, CLASS_CHAT_COMPONENT, byte.class));

        // Chat serializer //
        final Class<?> chatSerializerClass = Arrays.stream(CLASS_CHAT_COMPONENT.getClasses())
          .filter(JsonDeserializer.class::isAssignableFrom)
          .findAny()
          // fallback to the 1.7 class?
          .orElseGet(() -> {
            return nmsClass("ChatSerializer");
          });
        final Field gsonField = Arrays.stream(chatSerializerClass.getDeclaredFields())
          .filter(m -> Modifier.isStatic(m.getModifiers()))
          .filter(m -> m.getType().equals(Gson.class))
          .findFirst()
          .orElse(null);
        if(gsonField != null) {
          gsonField.setAccessible(true);
          gson = (Gson) gsonField.get(null);
        }
      }
    } catch(NoSuchMethodException | IllegalAccessException | IllegalArgumentException ex) {
      Knobs.logError("finding chat serializer", ex);
    }
    CHAT_PACKET_CONSTRUCTOR = chatPacketConstructor;
    MC_TEXT_GSON = gson;
    LEGACY_CHAT_PACKET_CONSTRUCTOR = legacyChatPacketConstructor;
  }

  private static Object mcTextFromComponent(final @NonNull Component message) {
    if(MC_TEXT_GSON == null || CLASS_CHAT_COMPONENT == null) {
      throw new IllegalStateException("Not supported");
    }
    final JsonElement json = BukkitPlatform.GSON_SERIALIZER.serializeToTree(message);
    try {
      return MC_TEXT_GSON.fromJson(json, CLASS_CHAT_COMPONENT);
    } catch(Throwable error) {
      Knobs.logError("converting adventure Component to MC Component", error);
      return null;
    }
  }

  /* package */ static class Chat extends PacketSendingHandler<CommandSender> implements Handler.Chat<CommandSender, Object> {
    
    @Override
    public boolean isAvailable() {
      return super.isAvailable() && CHAT_PACKET_CONSTRUCTOR != null;
    }

    @Override
    public Object initState(final @NonNull Component message) {
      final Object nmsMessage = mcTextFromComponent(message);
      if(nmsMessage == null) {
        return null;
      }

      try {
        return CHAT_PACKET_CONSTRUCTOR.invoke(nmsMessage, MESSAGE_TYPE_SYSTEM, NIL_UUID);
      } catch(Throwable throwable) {
        Knobs.logError("constructing MC chat packet", throwable);
        return null;
      }
    }
  }


  // Titles //
  private static final @Nullable Class<?> CLASS_TITLE_PACKET = Crafty.findNmsClass("PacketPlayOutTitle");
  private static final @Nullable Class<?> CLASS_TITLE_ACTION = Crafty.findNmsClass("PacketPlayOutTitle$EnumTitleAction"); // welcome to spigot, where we can't name classes? i guess?
  private static final MethodHandle CONSTRUCTOR_TITLE_MESSAGE; // (EnumTitleAction, IChatBaseComponent)
  private static final @Nullable MethodHandle CONSTRUCTOR_TITLE_TIMES = Crafty.optionalConstructor(CLASS_TITLE_PACKET, methodType(int.class, int.class, int.class));
  private static final @Nullable Object TITLE_ACTION_TITLE = Crafty.enumValue(CLASS_TITLE_ACTION, "TITLE", 0);
  private static final @Nullable Object TITLE_ACTION_SUBTITLE = Crafty.enumValue(CLASS_TITLE_ACTION, "SUBTITLE", 1);
  private static final @Nullable Object TITLE_ACTION_ACTIONBAR = Crafty.enumValue(CLASS_TITLE_ACTION, "ACTIONBAR");

  static {
    MethodHandle titlePacketConstructor = null;
    if(CLASS_TITLE_PACKET != null) {
      try {
        titlePacketConstructor = Crafty.LOOKUP.findConstructor(CLASS_TITLE_PACKET, methodType(void.class, CLASS_TITLE_ACTION, CLASS_CHAT_COMPONENT));
      } catch(NoSuchMethodException | IllegalAccessException ignore) {
      }
    }
    CONSTRUCTOR_TITLE_MESSAGE = titlePacketConstructor;

  }

  /* package */static class ActionBarModern extends PacketSendingHandler<Player> implements Handler.ActionBar<Player, Object> {

    @Override
    public boolean isAvailable() {
      return super.isAvailable() && TITLE_ACTION_ACTIONBAR != null;
    }

    @Override
    public Object initState(final @NonNull Component message) {
      try {
        return CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_ACTIONBAR, mcTextFromComponent(message));
      } catch(Throwable throwable) {
        Knobs.logError("constructing MC action bar packet", throwable);
        return null;
      }
    }
  }

  /* package */ static class ActionBar1_8thru1_11 extends PacketSendingHandler<Player> implements Handler.ActionBar<Player, Object> {

    @Override
    public Object initState(final @NonNull Component message) {
      // Action bar through the chat packet doesn't properly support formatting
      final TextComponent legacyMessage = TextComponent.of(LegacyComponentSerializer.legacy().serialize(message));
      try {
        return LEGACY_CHAT_PACKET_CONSTRUCTOR.invoke(mcTextFromComponent(legacyMessage), Chat.TYPE_ACTIONBAR);
      } catch(Throwable throwable) {
        Knobs.logError("constructing legacy MC action bar packet", throwable);
        return null;
      }
    }
  }

  /* package */ static class Title extends PacketSendingHandler<Player> implements Handler.Title<Player> {

    @Override
    public boolean isAvailable() {
      return super.isAvailable() && CONSTRUCTOR_TITLE_MESSAGE != null && CONSTRUCTOR_TITLE_TIMES != null;
    }

    @Override
    public void send(final @NonNull Player viewer, final net.kyori.adventure.title.@NonNull Title title) {
      final Object nmsTitleText = mcTextFromComponent(title.title());
      final Object nmsSubtitleText = mcTextFromComponent(title.subtitle());
      try {
        final Object titlePacket = CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_TITLE, nmsTitleText);
        final Object subtitlePacket = CONSTRUCTOR_TITLE_MESSAGE.invoke(TITLE_ACTION_SUBTITLE, nmsSubtitleText);
        Object timesPacket = null;

        final int fadeIn = Handler.Title.ticks(title.fadeInTime());
        final int stay = Handler.Title.ticks(title.stayTime());
        final int fadeOut = Handler.Title.ticks(title.fadeOutTime());

        if(fadeIn != -1 || stay != -1 || fadeOut != -1) {
          timesPacket = CONSTRUCTOR_TITLE_TIMES.invoke(fadeIn, stay, fadeOut);
        }

        send(viewer, subtitlePacket);
        if(timesPacket != null) {
          send(viewer, timesPacket);
        }
        send(viewer, titlePacket);
      } catch(Throwable throwable) {
        Knobs.logError("constructing legacy MC title packet", throwable);
      }
    }

    @Override
    public void clear(final @NonNull Player viewer) {
      viewer.sendTitle("", "", -1, -1, -1);
    }

    @Override
    public void reset(final @NonNull Player viewer) {
      viewer.resetTitle();
    }
  }

  /* package */ static class BossBarNameSetter implements BukkitBossBarListener.NameSetter {
    private static final Class<?> CLASS_CRAFT_BOSS_BAR = Crafty.findCraftClass("boss.CraftBossBar");
    private static final Class<?> CLASS_BOSS_BAR_ACTION = Crafty.findNmsClass("PacketPlayOutBoss$Action");
    private static final Object BOSS_BAR_ACTION_TITLE = Crafty.enumValue(CLASS_BOSS_BAR_ACTION, "UPDATE_NAME", 3);
    private static final MethodHandle CRAFT_BOSS_BAR_HANDLE;
    private static final MethodHandle NMS_BOSS_BATTLE_SET_NAME;
    private static final MethodHandle NMS_BOSS_BATTLE_SEND_UPDATE;

    static {
      MethodHandle craftBossBarHandle = null;
      MethodHandle nmsBossBattleSetName = null;
      MethodHandle nmsBossBattleSendUpdate = null;
      if(CLASS_CRAFT_BOSS_BAR != null && CLASS_CHAT_COMPONENT != null && BOSS_BAR_ACTION_TITLE != null) {
        try {
          final Field craftBossBarHandleField = Crafty.field(CLASS_CRAFT_BOSS_BAR, "handle");
          craftBossBarHandle = Crafty.LOOKUP.unreflectGetter(craftBossBarHandleField);
          final Class<?> nmsBossBattleType = craftBossBarHandleField.getType();
          nmsBossBattleSetName = Crafty.LOOKUP.findSetter(nmsBossBattleType, "title", CLASS_CHAT_COMPONENT);
          nmsBossBattleSendUpdate = Crafty.LOOKUP.findVirtual(nmsBossBattleType, "sendUpdate", methodType(void.class, CLASS_BOSS_BAR_ACTION));
        } catch(NoSuchFieldException | IllegalAccessException | NoSuchMethodException ex) {
          Knobs.logError("finding boss bar name operations", ex);
        }
      }
      CRAFT_BOSS_BAR_HANDLE = craftBossBarHandle;
      NMS_BOSS_BATTLE_SET_NAME = nmsBossBattleSetName;
      NMS_BOSS_BATTLE_SEND_UPDATE = nmsBossBattleSendUpdate;
    }

    @Override
    public boolean isAvailable() {
      return ENABLED && CLASS_CRAFT_BOSS_BAR != null && CRAFT_BOSS_BAR_HANDLE != null && NMS_BOSS_BATTLE_SET_NAME != null && NMS_BOSS_BATTLE_SEND_UPDATE != null;
    }

    @Override
    public void setName(final org.bukkit.boss.@NonNull BossBar bar, final @NonNull Component name) {
      try {
        final Object nmsBar = CRAFT_BOSS_BAR_HANDLE.invoke(bar);
        final Object mcText = mcTextFromComponent(name);
        // Boss bar was introduced MC 1.9, but the name setter method didn't exist until later versions, so for max compatibility we'll do field set and update separately
        NMS_BOSS_BATTLE_SET_NAME.invoke(nmsBar, mcText);
        NMS_BOSS_BATTLE_SEND_UPDATE.invoke(nmsBar, BOSS_BAR_ACTION_TITLE);
      } catch(final Error err) {
        throw err;
      } catch(final Throwable ex) {
        Knobs.logError("sending boss bar name change", ex);
      }
    }
  }
}
